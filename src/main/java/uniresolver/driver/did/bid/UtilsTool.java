package uniresolver.driver.did.bid;

import java.util.regex.Pattern;

public class UtilsTool {
    public static boolean encAddressValid(String encAddress) {
        boolean valid;
        try {
            if (null == encAddress || !isBid(encAddress)) {
                return  false;
            }
            String[] items = encAddress.split(":",-1);

            if (items.length != 3 && items.length != 4) {
                valid = false;
            }
            if (items.length == 3) {
                encAddress = items[2];
                if(isAc(encAddress)){
                    return  true;
                }
            } else {
                encAddress = items[3];
                String ac = items[2];
                if(!isAc(ac)){
                    return  false;
                }
            }
            String prifx = encAddress.substring(0, 2);

            if (!prifx.equals("ef") && !prifx.equals("zf")) {
                return  false;
            }

            String address = encAddress.substring(2, encAddress.length());
            byte[] base58_address = Base58.decode(address);
            if (base58_address.length != 22) {
                return  false;
            }
            valid = true;
        } catch (Exception e) {
            valid = false;
        }

        return valid;
    }
    public static boolean isAc(String value) {
        String rule = "^[a-z\\d]{4}+$";
        Pattern pattern = Pattern.compile(rule);
        return pattern.matcher(value).matches();
    }
    public static boolean isBid(String value) {
        String rule="^did:bid:[a-zA-Z0-9]*:*[a-zA-Z0-9]*";
        Pattern pattern = Pattern.compile(rule);
        return pattern.matcher(value).matches();
    }
    public static void main(String[] args) {
        String bid="did:bid:bca2";
        boolean s=encAddressValid(bid);
        boolean s2=isBid(bid);
        boolean s3=isAc("abd2");
        System.out.println(s);
        System.out.println(s2);
    }
}
