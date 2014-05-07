(ns searchzy.service.clean
  "A State monad-like thing.
   (State s a) :: initial state 's' -> (final value 'a', final state 's').
   Here, our state is either a single error or a collection of errors,
   depending on whether we choose 'short-circuit' or 'continue'."
  )

;;
;; Validation.
;; Pass all the args which need validation through a series of validation fns.
;; Validate each in turn, passing state as you go.
;; If you encounter a problem, either:
;;   -- continue, adding it to the state's aggregate collection of problems.
;;   -> short-circuit, returning the problem in the state.
;; When done, check the state.  If a problem, return it.  If not, continue.
;; 

(defn mk-cleaner-short-circuit
  "Higher-order fn which creates 'clean-' fns.
   Params:
     - oldput-key : name (Keyword) of the input arg group we wish to 'clean'
     - input-key  : name (Keyword) of the arg group to use in output
     - munge-fn   : fn :: input -> desired-output (of the arg group)
                    Upon validation error, must return nil.
     - error-fn   : fn :: (input, output) -> error-hashmap

   The created fn takes the hashmap of all args,
   attempts to 'munge' the desired subset,
   and returns either:
       [value nil] - upon success
       [nil error] - upon failure
  "
  [input-key output-key munge-fn error-fn]
  (fn [args]
    (let [i (get args input-key)
          o (munge-fn i)]
      (if (nil? o)
        [nil
         (error-fn i o)]
        [(-> args (dissoc input-key) (assoc output-key o))
         nil]))))

;;----------

(defn bind-short-circuit
  "Takes f and [value, previous-error].
   If there's been a previous-error, do nothing but pass it along.
   So, short-circuits if there's been any error."
  [f [val err]]
  (if (nil? err)
    (f val)
    [nil err]))

(defmacro short-circuit->>
  "Clean up input, short-circuiting upon first error."
  [val & fns]
  (let [fns (for [f fns] `(bind-short-circuit ~f))]
    `(->> [~val nil]
          ~@fns)))

;;----------

(defn mk-cleaner
  "Higher-order fn which creates 'clean-' fns.
   Params:
     - input-key  : name (Keyword) of the input arg group we wish to 'clean'
     - output-key : name (Keyword) of the arg group to use in output
     - munge-fn   : fn :: input -> desired-output (of the arg group)
                    Upon validation error, MUST RETURN NIL.
     - error-fn   : fn :: (input, output) -> error-hashmap

   The created fn takes the hashmap of all args,
   attempts to 'munge' the desired subset,
   and returns:
       [new-args nil]   - upon success
       [new-args error] - upon failure
  "
  [input-key output-key munge-fn error-fn]
  (fn [args]
    (let [i        (get args input-key)
          o        (munge-fn i)
          new-args (-> args (dissoc input-key) (assoc output-key o))
          err      (if (nil? o) (error-fn i o) nil)]
      [new-args, err])))

(defn bind-continue
  "If error, continue, adding error to list of errors.
   Take f and output of any previous fn (i.e. [value, err]).
   Always compute (f value).
   Add new-err to the errs."
  [f [val errs]]
  (let [[new-val new-err] (f val)]
    [new-val (if (nil? new-err)
               errs
               (cons new-err errs))]))
  
(defmacro gather->>
  "Clean up input, gathering errors."
  [val & fns]
  (let [fns (for [f fns] `(bind-continue ~f))]
    `(->> [~val ()]
          ~@fns)))
