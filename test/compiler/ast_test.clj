(ns compiler.ast-test
  (:require [clojure.test :refer :all]
            [compiler.core :refer :all]
            [compiler.ast :refer :all]))

(deftest ast-test
  (testing "AST test Sample."
    (is (= true true))))

;; (deftest for-test
;;   (testing "For test."
;;   ( is (->For (->Num 1) (->Num 2) (->Num 3) (->Num 4)))))

;; (deftest while-test
;;   (testing "While test."
;;     (is (->While (->Num 1) (->Num 2)))))

;; (deftest if-test
;;   (testing "If test."
;;     (is (->If (->Num 1) (->Num 2) (->Num 3)))))

;; (deftest return-test
;;   (testing "Return test."
;;     (is (->Return (->Num 1)))))

;; (deftest block-test
;;   (testing "Block test."  
;;     (is (->Block [(->Num 1) (->Num 2)]))))

;; (deftest function-test
;;   (testing "Function test."
;;     (is (->Function "main" [] (->Block [(->Num 1) (->Num 2)])))))

;; (deftest logical-op-test
;;   (testing "Logical op test."
;;     (is (->LogicalOp :&& (->Num 1) (->Num 2)))))

;; (deftest binary-op-test
;;   (testing "Binary op test."
;;     (is (->BinaryOp :+ (->Num 1) (->Num 2)))))

;; (deftest parens-test
;;   (testing "Parens test."
;;     (is (->Parens (->Num 1)))))

;; (deftest function-call-test
;;   (testing "Function call test."
;;     (is (->FunctionCall "main" [(->Num 1) (->Num 2)]))))

;; (deftest block-test
;;   (testing "Block test."  
;;     (is (->Block [(->Num 1) (->Num 2)]))))

;; (deftest interrupt-test
;;   (testing "Interrupt test."
;;     (is (->Interrupt))))

;; (deftest num-test
;;   (testing "Num test."
;;     (is (->Num 1))))

;; (deftest identifier-test
;;   (testing "Identifier test."
;;     (is (->Identifier "main"))))

;; (deftest type-test
;;   (testing "Type test."
;;     (is (->Type "int"))))

;; (deftest void-test
;;   (testing "Void test."
;;     (is (->Void))))

;; (deftest signed-test
;;   (testing "Signed test."
;;     (is (->Signed))))

;; (deftest unsigned-test
;;   (testing "Unsigned test."
;;     (is (->Unsigned))))

;; (deftest const-test
;;   (testing "Const test."
;;     (is (->Const (->Num 1)))))

;; (deftest typedef-test
;;   (testing "Typedef test."
;;     (is (->Typedef "int" (->Num 1)))))

;; (deftest goto-test
;;   (testing "Goto test."
;;     (is (->Goto "main"))))

;; (deftest interrupt-test
;;   (testing "Interrupt test."
;;     (is (->Interrupt))))

;; (deftest break-test
;;   (testing "Break test."
;;     (is (->Break))))

;; (deftest continue-test
;;   (testing "Continue test."
;;     (is (->Continue))))

;; (deftest return-test
;;   (testing "Return test."
;;     (is (->Return (->Num 1)))))

;; (deftest function-test
;;   (testing "Function test."
;;     (is (->Function "main" [] (->Block [(->Num 1) (->Num 2)])))))

