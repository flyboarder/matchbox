(ns matchbox.promise
  (:refer-clojure :exclude [promise -key -val map -deref])
  (:require
    [clojure.string :as str]
    [clojure.walk :as walk]
    [promesa.core :as prom]
    [matchbox.core :as mbox]
    [matchbox.promise.protocols :as proto]
    [matchbox.utils :as utils]))

;; this namespace should only be used with javascript (for now...)

(def promise prom/promise)

(def then prom/then)

(def catch prom/catch)

(def map prom/map)

(def chain prom/chain)

(def branch prom/branch)

;(def get-in mbox/get-in)

#?(:cljs
    (extend-protocol proto/Matchbox

      js.Firebase
      (get-in
        [ref korks]
        (let [path (utils/korks->path korks)]
          (if-not (seq path) ref (proto/-child ref path))))

      (parent
        [ref] (proto/-parent ref))

      prom/Promise
      (get-in
        [p korks]
        (then p #(get-in % korks)))

      (parent
        [p] (proto/-parent p))

      object
      (get-in
        [dat korks]
        (let [path (utils/korks->path korks)]
          (if-not (seq path) dat (proto/-child dat path))))

      (parent
        [dat] (proto/-parent dat))
))
