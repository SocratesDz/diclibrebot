(ns diclibrebot.core
  (:require [clojure.core.async :refer [<!!]]
            [clojure.string :as str]
            [environ.core :refer [env]]
            [morse.handlers :as h]
            [morse.polling :as p]
            [morse.api :as t]
            [diclibrebot.api :as dic-api])
  (:gen-class))

; TODO: fill correct token
;(def token (env :telegram-token))

(def token "511171474:AAFMUS8k5J3d5n38n92osu2PVY-ctfoCHs4")

(defn format-result [{title :title
                      definition :definition
                      example :example}]
  (str title "\n"
       definition "\n"
       "Ejemplos: " "\n" example))

(defn create-inline-result [{title :title
                            definition :definition :as result} id]
  {:type "article"
   :id (str id)
   :title title
   :input_message_content {:message_text (format-result result)}})

(defn answer-inline-result [{id :id query :query :as inline-msg}]
  (let [results (dic-api/search-definition query)
        formatted-results (map create-inline-result results (range (count results)))]
    formatted-results))

(def help-text "Para buscar una palabra escribe /define <palabra>.")
  (def not-found "No pudimos encontrar esa palabra :(")

(h/defhandler handler

  (h/command-fn "start"
                (fn [{{id :id :as chat} :chat}]
                  (println "Bot joined new chat: " chat)
                  (t/send-text token id "Welcome to diclibrebot!")))

  (h/command-fn "help"
                (fn [{{id :id :as chat} :chat}]
                  (println "Help was requested in " chat)
                  (t/send-text token id "Help is on the way")))

  (h/command-fn "define"
                (fn [{{id :id} :chat text :text :as message}]
                  (println "Definir: " message)
                  (let [clean-text (clojure.string/trim (clojure.string/replace-first text "/define" ""))
                        results (dic-api/search-definition clean-text)
                        results (map format-result results)
                        results (clojure.string/join "\n\n" results)
                        results (apply str results)
                        results (if (empty? clean-text) help-text results)
                        results (if (empty? results) not-found results)]
                    (t/send-text token id results))))

  ;; (h/inline-fn
  ;;  (fn [inline]
  ;;    (println "Inline message: " inline-msg)
  ;;    (let [results (answer-inline-result inline-msg)]
  ;;      (when (not (empty? results))
  ;;        (t/answer-inline token id formatted-results)))))
  )

(defn -main
  [& args]
  (when (str/blank? token)
    (println "Please provde token in TELEGRAM_TOKEN environment variable!")
    (System/exit 1))

  (println "Starting the diclibrebot")
  (<!! (p/start token handler)))
