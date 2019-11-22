### CURRIED ARGS ###
sum a b c = (+ a b c);
sum3_curried x y z = ((sum x y) z);
main = (sum3_curried 5 8 29);
