(ns matchbox.protocols.firebasequery
  (:refer-clojure :exclude [])
  (:require
    [clojure.string :as str]
    [clojure.walk :as walk]
    [promesa.core :as prom]
    [matchbox.core :as mbox]
    [matchbox.protocols :as proto]
    [matchbox.utils :as utils]))

#?(:cljs
    (extend-protocol proto/FirebaseQuery

      ;; Firebase Reference
      js.Firebase
      (-on
        ([ref event callback]
         (-on ref event callback matchbox.core/undefined))
        ([ref event callback failure]
         (.on ref event callback failure)))

      (-off
        ([ref event]
         (-off ref event matchbox.core/undefined))
        ([ref event callback]
         (.off ref event callback)))

      (-once
        ([ref event]
         (-once ref event matchbox.core/undefined))
        ([ref event callback]
         (-once ref event callback matchbox.core/undefined))
        ([ref event callback failure]
         (prom/promise (.once ref event callback failure))))

      (-orderbychild    [ref key] (.orderByChild ref key))

      (-orderbykey      [ref] (.orderByKey ref))

      (-orderbyvalue    [ref] (.orderByValue ref))

      (-orderbypriority [ref] (.orderByPriority ref))

      (-startat
        ([ref value]
         (-startat ref value nil))
        ([ref value key]
         (.startAt ref value key)))

      (-endat
        ([ref value]
         (-endat ref value nil))
        ([ref value key]
         (.endAt ref value key)))

      (-equalto
        ([ref value]
         (-equalto ref value nil))
        ([ref value key]
         (.equalTo ref value key)))

      (-limitfirst [ref limit] (.limitToFirst ref limit))

      (-limitlast  [ref limit] (.limitToLast ref limit))

      ;; Firebase DataSnapshot
      object
      (-on
        ([dat event callback]
         (-on dat event callback matchbox.core/undefined))
        ([dat event callback failure]
         (prom/chain dat -ref #(-on % event callback failure))))

      (-off
        ([dat event]
         (-off dat event matchbox.core/undefined))
        ([dat event callback]
         (prom/chain dat -ref #(-off % event callback))))

      (-once
        ([dat event]
         (-once dat event matchbox.core/undefined))
        ([dat event callback]
         (-once dat event callback matchbox.core/undefined))
        ([dat event callback failure]
         (prom/chain dat -ref #(-once % event callback failure))))

      (-orderbychild    [dat key] (prom/chain dat -ref #(-orderbychild % key)))

      (-orderbykey      [dat] (prom/chain dat -ref #(-orderbykey %)))

      (-orderbyvalue    [dat] (prom/chain dat -ref #(-orderbyvalue %)))

      (-orderbypriority [dat] (prom/chain dat -ref #(-orderbypriority %)))

      (-startat
        ([dat value]
         (-startat dat value nil))
        ([dat value key]
         (prom/chain dat -ref #(-startat % value key))))

      (-endat
        ([dat value]
         (-endat dat value nil))
        ([dat value key]
         (prom/chain dat -ref #(-endat % value key))))

      (-equalto
        ([dat value]
         (-equalto dat value nil))
        ([dat value key]
         (prom/chain dat -ref #(-equalto % value key))))

      (-limitfirst [dat limit] (prom/chain dat -ref #(-limitfirst % limit)))

      (-limitlast  [dat limit] (prom/chain dat -ref #(-limitlast % limit)))))
