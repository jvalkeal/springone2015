package demo;

import java.util.Date;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.cluster.leader.event.AbstractLeaderEvent;
import org.springframework.cloud.cluster.leader.event.OnGrantedEvent;
import org.springframework.cloud.cluster.leader.event.OnRevokedEvent;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.web.client.RestTemplate;
import org.springframework.yarn.annotation.OnContainerStart;
import org.springframework.yarn.annotation.YarnComponent;
import org.springframework.yarn.container.YarnContainerSupport;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

@SpringBootApplication
@EnableEurekaClient
@EnableHystrix
public class AdminContainerApplication {

	private static final Log log = LogFactory.getLog(AdminContainerApplication.class);

	@Autowired
    private Configuration configuration;

	@YarnComponent
	public static class StoreContainerWriter extends YarnContainerSupport
			implements ApplicationListener<AbstractLeaderEvent> {

		private AtomicBoolean active = new AtomicBoolean();

		@Autowired
		private HystrixStore storeIntegration;

		@OnContainerStart
		public ListenableFuture<?> writer() throws Exception {
			final WriterFuture future = new WriterFuture();

			getTaskScheduler().schedule(new FutureTask<Void>(new Runnable() {

				@Override
				public void run() {
					try {
						while (!future.interrupted) {
							if (active.get()) {
								storeIntegration.writeEntity("Time is " + System.currentTimeMillis());
							}
							Thread.sleep(1000);
						}
					} catch (Exception e) {
						log.error("Got error for write loop, exiting via future", e);
						future.set(false);
					}
				}
			}, null), new Date());

			return future;
		}

		@Override
		public void onApplicationEvent(AbstractLeaderEvent event) {
			if (event instanceof OnGrantedEvent) {
				log.info("XXXX Granting leader");
				active.set(true);
			} else if (event instanceof OnRevokedEvent){
				log.info("XXXX Revoking leader");
				active.set(false);
			}
		}

	}

	@Component
	static class HystrixStore {

		@Autowired
		@Qualifier("loadBalancedRestTemplate")
		private RestTemplate restTemplate;

		@HystrixCommand(fallbackMethod = "fallbackWriteEntity")
		public String writeEntity(String entity) {
			restTemplate.postForObject("http://storecontainer/store", entity, String.class);
			return "ok";
		}

		public String fallbackWriteEntity(String entity) {
			return "error";
		}
	}

	static class WriterFuture extends SettableListenableFuture<Boolean> {

		boolean interrupted = false;

		@Override
		protected void interruptTask() {
			interrupted = true;
		}
	}

	public static void main(String[] args) {
		SpringApplication.run(AdminContainerApplication.class, args);
	}

}
