(ns matchbox.promise
  (:refer-clojure :exclude [promise map deref reset! swap!])
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

#?(:cljs
    (defn --deref
      ([in]
       (chain in proto/-val mbox/hydrate))
      ([in state]
       (--deref in state mbox/undefined))
      ([in state callback]
       (proto/-val in #(comp (cljs.core/reset! state (mbox/hydrate (proto/-val %)))
                              (callback %))))))
#?(:cljs
    (defn --deref-list
      ([ref]
       (proto/-once ref "value" mbox/get-children))
      ([ref state]
       (--deref ref state mbox/get-children))
      ([ref state callback]
       (--deref ref state #(callback (mbox/get-children %))))))

#?(:cljs
    (defn deref [& args]
       (apply proto/-deref args)))

#?(:cljs
    (defn merge! [& args]
       (apply proto/-merge! args)))

#?(:cljs
    (defn reset! [& args]
       (apply proto/-reset! args)))

#?(:cljs
    (defn swap! [& args]
       (apply proto/-swap! args)))

#?(:cljs
    (extend-protocol proto/Matchbox

      ;; Firebase Reference
      js.Firebase
      (get-in
        [ref korks]
        (mbox/get-in ref korks))

      (parent
        [ref]
        (proto/-parent ref))

      (-deref
        ([ref]
         (--deref ref))
        ([ref state]
         (--deref ref state))
        ([ref state callback]
         (--deref ref state callback)))

      (-deref-list
        ([ref]
         (--deref-list ref))
        ([ref state]
         (--deref-list ref state))
        ([ref state callback]
         (--deref-list ref state callback)))

      (-reset!
        ([ref val]
         (reset! ref val mbox/undefined))
        ([ref val callback]
         (mbox/reset! ref val callback)))

      (-swap!
        [ref fn args]
        (apply (partial mbox/swap! ref fn) args))

      (-merge!
        ([ref val]
         (merge! ref val mbox/undefined))
        ([ref val callback]
         (mbox/merge! ref val callback)))

      ;; Firebase Promise
      prom/Promise
      (get-in
        [p korks]
        (then p #(get-in % korks)))

      (parent
        [p]
        (then p proto/-parent))

      (-deref
        ([p]
         (--deref p))
        ([p state]
         (--deref p state))
        ([p state callback]
         (--deref p state callback)))

      (-deref-list
        ([p]
         (--deref-list p))
        ([p state]
         (--deref-list p state))
        ([p state callback]
         (--deref-list p state callback)))

      (-reset!
        ([p val]
         (reset! p val mbox/undefined))
        ([p val callback]
         (then p #(reset! % val callback))))

      (-swap!
        ([p fn args]
         (then p #(swap! % fn args))))

      (-merge!
        ([p val]
         (merge! p val mbox/undefined))
        ([p val callback]
         (then p #(merge! % val callback))))

      ;; Firebase DataSnapshot
      object
      (get-in
        [ref korks]
        (mbox/get-in ref korks))

      (parent
        [dat]
        (proto/-parent dat))

      (-deref
        ([dat]
         (-> dat proto/-val mbox/hydrate))
        ([dat state]
         (--deref dat state))
        ([dat state callback]
         (--deref dat state callback)))

      (-deref-list
        ([dat]
         (mbox/get-children dat))
        ([dat state]
         (--deref-list dat state))
        ([dat state callback]
         (--deref-list dat state callback)))

      (-reset!
        ([dat val]
         (reset! dat val mbox/undefined))
        ([dat val callback]
         (reset! (proto/-ref dat) val callback)))

      (-swap!
        ([dat fn args]
         (swap! (proto/-ref dat) fn args)))

      (-merge!
        ([dat val]
         (merge! dat val mbox/undefined))
        ([dat val callback]
         (merge! (proto/-ref dat) val callback)))

))
