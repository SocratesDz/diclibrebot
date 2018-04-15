(ns diclibrebot.core
  (:require [clojure.core.async :refer [<! <!! go-loop timeout]]
            [clojure.string :as s]
            [environ.core :refer [env]]
            [morse.handlers :as h]
            [morse.polling :as p]
            [morse.api :as t]
            [diclibrebot.api :as dic-api]
            [compojure.core :refer [defroutes POST ANY]]
            [compojure.handler :refer [site]]
            [compojure.route :as route]
            [ring.adapter.jetty :as jetty])
  (:gen-class))

(def token (env :telegram-token))
(def base-url (env :domain))

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
(def welcome-text "Bienvenido a Diccionario Libre Bot!")

(h/defhandler handler

  (h/command-fn "start"
                (fn [{{id :id :as chat} :chat}]
                  (println "Bot joined new chat: " chat)
                  (t/send-text token id (str welcome-text "\n" help-text))))

  (h/command-fn "help"
                (fn [{{id :id :as chat} :chat}]
                  (println "Help was requested in " chat)
                  (t/send-text token id "Help is on the way")))

  (h/command-fn "define"
                (fn [{{id :id} :chat text :text :as message}]
                  (println "Definir: " message)
                  (let [clean-text (s/trim (s/replace-first text "/define" ""))
                        results (dic-api/search-definition clean-text)
                        results (map format-result results)
                        results (if (empty? clean-text) '(help-text) results)
                        results (if (empty? results) '(not-found) results)]
                    (doseq [r results]
                      (t/send-text token id r))))))

(defroutes app
  (POST "/debug" {body :body} (clojure.pprint/pprint body))
  (POST "/handler" {{updates :result} :body} (map handler updates))
  (ANY "*" [] (route/not-found "Not Found")))

;; I can't use this with heroku :(
(defn webhook-bot [port]
  (let [port (Integer. (or port (env :port)))]
    (t/set-webhook token (str base-url ":" port "/handler"))
    (jetty/run-jetty (site #'app) {:port port :join? false})))

(defn polling-bot []
  (println "Starting diclibrebot.")
  (go-loop [ch (p/start token handler)]
    (<! (timeout 5000))
    (if (nil? (<! ch))
      (do
        (p/stop ch)
        (println "Bot restarted!")
        (recur (p/start token handler)))
      (recur ch))))
  ;;(<!! (p/start token handler)))

(defn -main [& args]
  (when (str/blank? token)
    (println "Please provide token in TELEGRAM_TOKEN environment variable!")
    (System/exit 1))

  ;; Can't use webhook because heroku keeps changing the ports :(
  (polling-bot)
  (Thread/sleep Long/MAX_VALUE))
