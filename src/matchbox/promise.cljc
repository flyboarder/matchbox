(ns matchbox.promise
  (:refer-clojure :exclude [promise map deref reset! swap! conj! dissoc! take take-last])
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
    (defn conj! [& args]
       (apply proto/-conj! args)))

#?(:cljs
    (defn reset! [& args]
       (apply proto/-reset! args)))

#?(:cljs
    (defn swap! [& args]
       (apply proto/-swap! args)))

#?(:cljs
    (defn dissoc! [& args]
       (apply proto/-dissoc! args)))
#?(:cljs
    (defn take [ref limit]
      (proto/-limitfirst ref limit)))

#?(:cljs
    (defn take-last [ref limit]
      (proto/-limitlast ref limit)))

#?(:cljs
    (extend-protocol proto/Matchbox

      ;; Firebase Reference
      js.Firebase
      (-get-in
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

      (-conj!
        ([ref val]
         (conj! ref val mbox/undefined))
        ([ref val callback]
         (mbox/conj! ref val callback)))

      (-dissoc!
        ([ref]
         (dissoc! ref mbox/undefined))
        ([ref callback]
         (mbox/dissoc! ref callback)))

      (order-priority [ref] (mbox/order-by-priority ref))

      (order-key [ref] (mbox/order-by-key ref))

      (order-value [ref] (mbox/order-by-value ref))

      (order-child [ref child] (mbox/order-by-child ref child))

      (start-at
        ([ref val]
         (mbox/start-at ref val))
        ([ref val key]
         (mbox/start-at ref val key)))

      (end-at
        ([ref val]
         (mbox/end-at ref val))
        ([ref val key]
         (mbox/end-at ref val key)))

      (equal-to
        ([ref val]
         (mbox/equal-to ref val))
        ([ref val key]
         (mbox/equal-to ref val key)))

      ;; Firebase Promise
      prom/Promise
      (-get-in
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

      (-conj!
        ([p val]
         (conj! p val mbox/undefined))
        ([p val callback]
         (then p #(conj! % val callback))))

      (-dissoc!
        ([p]
         (dissoc! p mbox/undefined))
        ([p callback]
         (then p #(dissoc! % callback))))

      (order-priority [p] (then p proto/order-priority))

      (order-key [p] (then p proto/order-key))

      (order-value [p] (then p proto/order-value))

      (order-child [p child] (then p #(proto/order-child % child)))

      (start-at
        ([p val]
         (then p #(proto/start-at % val)))
        ([p val key]
         (then p #(proto/start-at % val key))))

      (end-at
        ([p val]
         (then p #(proto/end-at % val)))
        ([p val key]
         (then p #(proto/end-at % val key))))

      (equal-to
        ([p val]
         (then p #(proto/equal-to % val)))
        ([p val key]
         (then p #(proto/equal-to % val key))))

      ;; Firebase DataSnapshot
      object
      (-get-in
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

      (-conj!
        ([dat val]
         (conj! dat val mbox/undefined))
        ([dat val callback]
         (conj! (proto/-ref dat) val callback)))

      (-dissoc!
        ([dat]
         (dissoc! dat mbox/undefined))
        ([dat callback]
         (mbox/dissoc! (proto/-ref dat) callback)))

      (order-priority [dat] (proto/order-priority (proto/-ref dat)))

      (order-key [dat] (proto/order-key (proto/-ref dat)))

      (order-value [dat] (proto/order-value (proto/-ref dat)))

      (order-child [dat child] (proto/order-child (proto/-ref dat) child))

      (start-at
        ([dat val]
         (mbox/start-at (proto/-ref dat) val))
        ([dat val key]
         (mbox/start-at (proto/-ref dat) val key)))

      (end-at
        ([dat val]
         (mbox/end-at (proto/-ref dat) val))
        ([dat val key]
         (mbox/end-at (proto/-ref dat) val key)))

      (equal-to
        ([dat val]
         (mbox/equal-to (proto/-ref dat) val))
        ([dat val key]
         (mbox/equal-to (proto/-ref dat) val key)))

))
