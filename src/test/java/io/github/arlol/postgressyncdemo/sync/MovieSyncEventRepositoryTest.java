package io.github.arlol.postgressyncdemo.sync;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import io.github.arlol.postgressyncdemo.movie.Movie;
import io.github.arlol.postgressyncdemo.movie.MovieRepository;

@SpringBootTest
@ActiveProfiles("postgres")
public class MovieSyncEventRepositoryTest {

	@Autowired
	MovieSyncEventRepository repository;
	@Autowired
	MovieRepository movieRepository;

	@Test
	void testFindAll() throws Exception {
		assertThat(repository.findAll()).isEmpty();
	}

	@Test
	void testSave() throws Exception {
		MovieSyncEvent event = repository
				.save(MovieSyncEvent.builder().action("I").build());
		event = repository.save(event.toBuilder().action("U").build());
	}

	@Test
	void testFindAndDeleteNextSyncEvent() throws Exception {
		assertThat(repository.findAll()).isEmpty();

		var nextSyncEvent = repository.findAndDeleteNextSyncEvent();
		assertThat(nextSyncEvent).isEmpty();

		Movie movie = Movie.builder().title("Batman").build();
		movie = movieRepository.save(movie);
		movie = movieRepository
				.save(movie.toBuilder().title("Batman Begins").build());
		movieRepository.delete(movie);

		assertThat(repository.findAll()).isNotEmpty();

		nextSyncEvent = repository.findAndDeleteNextSyncEvent();
		assertThat(nextSyncEvent).isNotEmpty();
		assertThat(nextSyncEvent.get().getAction()).isEqualTo("I");
		assertThat(nextSyncEvent.get().getMovieId()).isEqualTo(movie.getId());

		nextSyncEvent = repository.findAndDeleteNextSyncEvent();
		assertThat(nextSyncEvent).isNotEmpty();
		assertThat(nextSyncEvent.get().getAction()).isEqualTo("U");
		assertThat(nextSyncEvent.get().getMovieId()).isEqualTo(movie.getId());

		nextSyncEvent = repository.findAndDeleteNextSyncEvent();
		assertThat(nextSyncEvent).isNotEmpty();
		assertThat(nextSyncEvent.get().getAction()).isEqualTo("D");
		assertThat(nextSyncEvent.get().getMovieId()).isEqualTo(movie.getId());

		assertThat(repository.findAll()).isEmpty();
	}

}
