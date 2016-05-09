(ns matchbox.promise
  (:refer-clojure :exclude [promise map deref])
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
    (defn -get-in
      [in korks]
      (let [path (utils/korks->path korks)]
        (if-not (seq path) in (proto/-child in path)))))

#?(:cljs
    (defn --deref
      ([ref]
       (then (proto/-val ref) mbox/hydrate)
       )
      ([ref state]
       (--deref ref state mbox/undefined))
      ([ref state callback]
       (proto/-val ref #(comp (reset! state (mbox/hydrate (proto/-val %)))
                              callback)))))

#?(:cljs
    (defn deref [& args]
       (apply proto/-deref args)))

#?(:cljs
    (extend-protocol proto/Matchbox

      ;; Firebase Reference
      js.Firebase
      (get-in
        [ref korks]
        (-get-in ref korks))

      (parent
        [ref]
        (proto/-parent ref))

      (-deref
        ([ref]
         (--deref ref))
        ([ref state]
         (--deref ref state)))

      ;; Firebase Promise
      prom/Promise
      (get-in
        [p korks]
        (then p #(get-in % korks)))

      (parent
        [p]
        (proto/-parent p))

      (-deref
        ([ref]
         (--deref ref))
        ([ref state]
         (--deref ref state)))

      ;; Firebase DataSnapshot
      object
      (get-in
        [ref korks]
        (-get-in ref korks))

      (parent
        [dat]
        (proto/-parent dat))

      (-deref
        ([dat]
         (-> dat proto/-val mbox/hydrate))
        ([dat state]
         (--deref dat state)))
))
