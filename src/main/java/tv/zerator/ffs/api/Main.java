package tv.zerator.ffs.api;

import alexmog.apilib.ApiServer;
import tv.zerator.ffs.api.v1.ApiV1;

public class Main extends ApiServer {
	private static Main instance;

	public static Main getInstance() {
		return instance;
	}
	
	public static void main(String[] args) throws Exception {
		new Main().start(null, new ApiEndpointBuilder("/v1", new ApiV1(instance.getConfig())));
	}
	
	public Main() {
		instance = this;
	}
}
