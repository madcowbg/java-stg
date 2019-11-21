# define one function
sum3 x y z = (+ x y z);

# define lambda
lam x z = \ y -> (sum3 x y z);

# apply to values
main = ((lam 34 2) 6);