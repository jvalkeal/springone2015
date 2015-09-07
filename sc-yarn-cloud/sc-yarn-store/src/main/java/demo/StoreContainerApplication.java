package demo;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.hadoop.store.PartitionDataStoreWriter;
import org.springframework.data.hadoop.store.config.annotation.EnableDataStorePartitionTextWriter;
import org.springframework.data.hadoop.store.config.annotation.SpringDataStoreTextWriterConfigurerAdapter;
import org.springframework.data.hadoop.store.config.annotation.builders.DataStoreTextWriterConfigurer;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableEurekaClient
public class StoreContainerApplication {

	private static final Log log = LogFactory.getLog(StoreContainerApplication.class);

	@RestController
	@RequestMapping("/store")
	static class StoreController {

		@Autowired
		private PartitionDataStoreWriter<String, Map<String, Object>> writer;

		@RequestMapping(method = RequestMethod.POST)
		public HttpEntity<Void> write(@RequestBody String entity) {
			log.info("Trying to write entity: " + entity);
			try {
				writer.write(entity);
			} catch (IOException e) {
				throw new WriteException(e);
			}
			return new ResponseEntity<Void>(HttpStatus.OK);
		}

	}

	@SuppressWarnings("serial")
	@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR, reason = "Writer error")
	private static class WriteException extends RuntimeException {

		public WriteException(Throwable cause) {
			super(cause);
		}

	}

	@Configuration
	@EnableDataStorePartitionTextWriter
	static class StoreConfig extends SpringDataStoreTextWriterConfigurerAdapter {

		@Override
		public void configure(DataStoreTextWriterConfigurer config)
				throws Exception {
			config
				.basePath("/tmp/store")
				.idleTimeout(60000)
				.inWritingSuffix(".tmp")
				.withPartitionStrategy()
					.map("dateFormat('yyyy/MM/dd/HH/mm', timestamp)")
					.and()
				.withNamingStrategy()
					.name("data")
					.uuid()
					.rolling()
					.name("txt", ".")
					.and()
				.withRolloverStrategy()
					.size("1M");
		}
	}

	public static void main(String[] args) {
		SpringApplication.run(StoreContainerApplication.class, args);
	}

}
