package tv.zerator.ffs.api.rabbitmq;

import org.codehaus.jackson.annotate.JsonMethod;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.mrbean.MrBeanModule;

import alexmog.apilib.rabbitmq.packets.Packet;
import io.netty.buffer.ByteBuf;

public class TopicPacket extends Packet {
	public String topic;
	public String message;
	private static final ObjectMapper mMapper;
    
	static {
		mMapper = new ObjectMapper().enable(DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY).enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL)
		        .setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL).setVisibility(JsonMethod.FIELD, Visibility.ANY);
	    mMapper.registerModule(new MrBeanModule());
	    mMapper.disableDefaultTyping();
	}

	public TopicPacket() {
		packetId = 1;
	}
	
	public TopicPacket(String topic, Object message) throws JsonGenerationException, JsonMappingException, IOException {
		this();
		this.topic = topic;
		this.message = mMapper.writeValueAsString(message);
	}
	
	@Override
	public void writeData(ByteBuf buf) {
		writeUTF8(buf, topic);
		writeUTF8(buf, message);
	}

	@Override
	public void readData(ByteBuf buf) {
		topic = readUTF8(buf);
		message = readUTF8(buf);
	}

}
