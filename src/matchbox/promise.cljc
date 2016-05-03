(ns matchbox.promise
  (:require
    [clojure.string :as str]
    [clojure.walk :as walk]
    [matchbox.utils :as utils]))

;; macros
(defmacro promise-> [& args]
    (let [[err args] (utils/extract :catch args)
          make-call (fn [i & rest]
                      (if (zero? i)
                        `(.once ~@rest "value")
                        `(.then ~@rest)))
          calls (map-indexed make-call args)]
      (if err
        `(-> ~@calls (.catch ~err))
        `(-> ~@calls))))
