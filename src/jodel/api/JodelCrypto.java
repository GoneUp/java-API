package jodel.api;

import java.net.*;
import java.security.MessageDigest;

import okio.ByteString;

public class JodelCrypto {
	private static String sig = "3082036130820249a003020102020431bd30eb300d06092a864886f70d01010b05003061310b3009060355040613024445310f300d060355040813064265726c696e310f300d060355040713064265726c696e310e300c060355040a130574656c6c4d3110300e060355040b1307416e64726f6964310e300c0603550403130574656c6c4d301e170d3134303430333139303332355a170d3431303831393139303332355a3061310b3009060355040613024445310f300d060355040813064265726c696e310f300d060355040713064265726c696e310e300c060355040a130574656c6c4d3110300e060355040b1307416e64726f6964310e300c0603550403130574656c6c4d30820122300d06092a864886f70d01010105000382010f003082010a02820101009c4e279690f38ab6e9431cf3d66e3a95221dcafd477e0a71b6c0f58e073a2dedb84d3eea7dab1e1ac85e523be9c25ebcd25ad1e87d8a253d5b791f96211020ab250e593d3ccbdc9153e1aa97e921ef335745ffc7f547c5daa6a6563ab7fe988195066062626e4a45402050ce3e4d9fdf1f168480f99cde04f906b69ff33690dd869a10d7ee3e6a935175fd5f2582ccfe59d2158fd67facd700e708660db4b78189b37f6aaba4e0c77fce0db357e1f62a7759903340edc9b29401e4e640eb8c389f5e7f16325297cea71f3d0ea59d5d189e22edaadcf258e61f84f396092d5fa1952e0a93b5355171987ba9d30426d88134547665abfacf4e4ad3744fdc2fc1530203010001a321301f301d0603551d0e04160414d24cc5b5c4df1c6c8eb8ceb1ba156619c23a0301300d06092a864886f70d01010b050003820101002f7595e91fab930daa50c14ff3ab0c14c7857eb11ed31f4c8a4ee2398d1323eb9c6c8a596fc258fdeaa1d5fb458119db497fc9d2a3d2bdb30e45b92d0686e244ebed8303cc0659c32ab6b71aaac6b3a3eacb1f97407c250822186dfc6cf406b9ccb643434905b90d4d53881b8f7a20568f7b545c8ed38dbcc49d46c057b7ef74445111de931161862acbbf528c066b445d02e4a2f038d7e1168edf8b6f4b7f4b8c158684e233a90fc9302e216974c6cb8c686f773890540c9b0a17fc1f4c70f232913440fb2ff6b10b5ab4f07356e4e088e09b9022f71fe813f10087a9a349a7359552611f4c1d9acc237a850f3000bb2b89939cb964c56026a054f0365ba6d9";

	   static {
		   System.out.println(System.getProperty("java.library.path"));
	       // System.loadLibrary("hmac");
	    }
	
	public static void genSecret() throws Exception {
		URI uri = new URI("https://api.go-tellm.com:443/api");

		byte[] byteSig = hexStringToByteArray(sig);
		
		MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
		messageDigest.update(byteSig);

		byteSig = generate(hex(messageDigest.digest())).getBytes("UTF-8");
	}
	
    private static String hex(byte[] arrby) {
        return ByteString.of(arrby).hex();
    }

	private static native String generate(String input);
	
	public static byte[] hexStringToByteArray(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}
}
