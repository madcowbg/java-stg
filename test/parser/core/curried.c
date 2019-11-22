sum x y = (+ x y);
sum3_curried x y z = ((sum x y) z);
sum3_snd = (sum (sum x y) z);
main = (sum3_curried 5 8 29);