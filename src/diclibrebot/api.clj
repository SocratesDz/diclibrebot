(ns diclibrebot.api
  (:gen-class)
  (:require [clojure.string :refer [lower-case replace]]
            [reaver :refer [parse extract-from text attr]]))

(def base-url "http://diccionariolibre.com/")
(def definition-path "definicion/")

(defn clean-search-results [results]
  (if (-> (first results)
          (:title)
          (nil?))
    '()
    results))

(defn search-definition
  "Return search results from diccionariolibre.com.
  They are represented as {:title :definition :example}"
  [word]
  (if (not (nil? word))
    (let [word (clojure.string/trim word)
          url-to-search (str
                         base-url
                         definition-path
                         (lower-case (replace word " " "-")))]
      (-> (slurp url-to-search)
          (parse)
          (extract-from ".dl-article"
                        [:title :definition :example]
                        ".title-definition" text
                        ".definition-word" text
                        ".definition-example" text)
          (clean-search-results)))
    '()))
