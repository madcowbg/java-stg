sum x y = (+ x y);
sum3_snd x y z = (sum (sum x y) z);
main = (sum3_snd 5 8 29);