package tv.zerator.ffs.api;

import java.io.IOException;

import com.rabbitmq.client.BuiltinExchangeType;

import alexmog.apilib.ApiServer;
import alexmog.apilib.managers.Managers.Manager;
import alexmog.apilib.managers.RabbitMQManager;
import tv.zerator.ffs.api.v1.ApiV1;

public class Main extends ApiServer {
	private static Main instance;
	@Manager
	private static RabbitMQManager mRabbitMQManager;

	public static Main getInstance() {
		return instance;
	}
	
	public static void main(String[] args) throws Exception {
		new Main().start(null, new ApiEndpointBuilder("/v1", new ApiV1(instance.getConfig())));
	}
	
	private void initRabbitMQ() throws IOException {
		LOGGER.info("Initialization of the Notifications channel");
		mRabbitMQManager.getChannel().exchangeDeclare("Pub", BuiltinExchangeType.FANOUT, true);
		LOGGER.info("Initialization of the Pub channel done.");
	}
	
	public Main() throws Exception {
		instance = this;
		initRabbitMQ();
	}
}
