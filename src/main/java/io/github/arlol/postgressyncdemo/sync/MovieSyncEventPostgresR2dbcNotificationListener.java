package io.github.arlol.postgressyncdemo.sync;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import io.r2dbc.postgresql.api.PostgresqlConnection;
import io.r2dbc.postgresql.api.PostgresqlResult;
import io.r2dbc.spi.ConnectionFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

@Service
public class MovieSyncEventPostgresR2dbcNotificationListener
		implements AutoCloseable, InitializingBean {

	private Disposable subscription;
	private ConnectionFactory connectionFactory;
	private MovieSyncServiceTrigger trigger;
	private Boolean enabled;

	public MovieSyncEventPostgresR2dbcNotificationListener(
			ConnectionFactory connectionFactory,
			MovieSyncServiceTrigger trigger,
			Environment environment
	) {
		this.connectionFactory = connectionFactory;
		this.trigger = trigger;
		enabled = environment.getProperty(
				"postgres-sync-demo.movie-sync-service.enabled",
				Boolean.class,
				Boolean.TRUE
		);
	}

	@Override
	public void close() throws Exception {
		if (subscription != null) {
			subscription.dispose();
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (enabled) {
			start();
		}
	}

	public void start() throws Exception {
		subscription = Mono.from(connectionFactory.create())
				.flatMapMany(connection -> {
					if (!(connection instanceof PostgresqlConnection)) {
						return connection.close();
					}
					PostgresqlConnection pgConnection = (PostgresqlConnection) connection;
					return pgConnection
							.createStatement("LISTEN movie_sync_event_channel")
							.execute()
							.flatMap(PostgresqlResult::getRowsUpdated)
							.thenMany(pgConnection.getNotifications())
							.doOnNext(notification -> trigger.trigger());
				})
				.subscribe();
	}

}
