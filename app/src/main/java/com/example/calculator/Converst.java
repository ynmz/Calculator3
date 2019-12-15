package com.example.calculator;

public class Converst {
        public static int level(String str) {
            int le=0;
            switch(str) {
                case "cm3":
                case "cm":
                    le=1;break;
                case "dm":
                case "dm3":
                    le=2;break;
                case "m":
                case "m3":
                    le=3;break;
                default:

            }
            return le;
        }
        public static String scale(String str,String first,String second) {
            String resultString=null;
            Integer inputInerger=Integer.valueOf(str);
            if(first.equals(")10")) {
                if(second.equals(")2")) {
                    resultString=Integer.toBinaryString(inputInerger);
                }
                else if(second.equals(")8")) {
                    resultString=Integer.toOctalString(inputInerger);
                }
                else if(second.equals(")16")) {
                    resultString=Integer.toHexString(inputInerger);
                }
                else
                    resultString=Integer.toString(inputInerger);
            }
            else if(first.equals(")2")) {
                if(second.equals(")10")) {
                    Integer intResult = 0;
                    int len = str.length();
                    for (int i = 1; i <= len; i++) {
                        int dt = Integer.parseInt(str.substring(i - 1, i));
                        intResult += (int) Math.pow(2, len - i) * dt;
                    }
                    Integer integer = (Integer) inputInerger;
                    resultString = integer.toString();
                }
            }

            return resultString;

        }
        public static String converst(String str,String first,String second) {
            Double result;
            String resultString=null;
            double input=Double.valueOf(str);
            int temp=level(first)-level(second);
            if(first.equals("cm3")||first.equals("dm3")||first.equals("m3")) {
                result=input*(Math.pow(1000,temp));
                resultString=result.toString();
            }
            else if(first.equals("cm")||first.equals("dm")||first.equals("m")) {
                result=input*(Math.pow(10,temp));
                resultString=result.toString();
            }
            else {
                resultString=scale(str,first,second);

            }

            return resultString;
        }

    }
