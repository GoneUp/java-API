package jodel.api;
import org.json.JSONException;
import org.json.JSONObject;


public class JodelLocation {
	public String country;
	public String city;
	public double lng;
	public double lat;
	
	
	public JodelLocation(){}
	
	public JodelLocation(String city, double lng, double lat, String country) {
		this.city = city;
		this.lng = lng;
		this.country = country;
		this.lat = lat;
	}
	
	public JSONObject toJSONObj() throws JSONException{
		JSONObject jsonLocInner = new JSONObject();
		jsonLocInner.put("lat", lat);
		jsonLocInner.put("lng", lng);
		
		JSONObject jsonLoc = new JSONObject();
		jsonLoc.put("loc_accuracy", 20.1);
		jsonLoc.put("city", city);
		jsonLoc.put("country", country);
		jsonLoc.put("loc_coordinates", jsonLocInner);
		return jsonLoc;
	}
	
}
