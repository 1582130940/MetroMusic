package code.name.monkey.retromusic

import androidx.room.Room
import code.name.monkey.retromusic.auto.AutoMusicProvider
import code.name.monkey.retromusic.db.MIGRATION_23_24
import code.name.monkey.retromusic.db.RetroDatabase
import code.name.monkey.retromusic.fragments.LibraryViewModel
import code.name.monkey.retromusic.fragments.albums.AlbumDetailsViewModel
import code.name.monkey.retromusic.fragments.artists.ArtistDetailsViewModel
import code.name.monkey.retromusic.fragments.genres.GenreDetailsViewModel
import code.name.monkey.retromusic.fragments.playlists.PlaylistDetailsViewModel
import code.name.monkey.retromusic.model.Genre
import code.name.monkey.retromusic.repository.AlbumRepository
import code.name.monkey.retromusic.repository.ArtistRepository
import code.name.monkey.retromusic.repository.GenreRepository
import code.name.monkey.retromusic.repository.LastAddedRepository
import code.name.monkey.retromusic.repository.PlaylistRepository
import code.name.monkey.retromusic.repository.RealAlbumRepository
import code.name.monkey.retromusic.repository.RealArtistRepository
import code.name.monkey.retromusic.repository.RealGenreRepository
import code.name.monkey.retromusic.repository.RealLastAddedRepository
import code.name.monkey.retromusic.repository.RealPlaylistRepository
import code.name.monkey.retromusic.repository.RealRepository
import code.name.monkey.retromusic.repository.RealRoomRepository
import code.name.monkey.retromusic.repository.RealSearchRepository
import code.name.monkey.retromusic.repository.RealSongRepository
import code.name.monkey.retromusic.repository.RealTopPlayedRepository
import code.name.monkey.retromusic.repository.Repository
import code.name.monkey.retromusic.repository.RoomRepository
import code.name.monkey.retromusic.repository.SongRepository
import code.name.monkey.retromusic.repository.TopPlayedRepository
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

private val roomModule = module {
    single {
        Room.databaseBuilder(
            context = androidContext(),
            klass = RetroDatabase::class.java,
            name = "playlist.db"
        )
            .addMigrations(MIGRATION_23_24)
            .build()
    }

    factory {
        get<RetroDatabase>().playlistDao()
    }

    factory {
        get<RetroDatabase>().playCountDao()
    }

    factory {
        get<RetroDatabase>().historyDao()
    }

    single {
        RealRoomRepository(playlistDao = get(), playCountDao = get(), historyDao = get())
    } bind RoomRepository::class
}
private val autoModule = module {
    single {
        AutoMusicProvider(
            mContext = androidContext(),
            songsRepository = get(),
            albumsRepository = get(),
            artistsRepository = get(),
            genresRepository = get(),
            playlistsRepository = get(),
            topPlayedRepository = get()
        )
    }
}
private val mainModule = module {
    single {
        androidContext().contentResolver
    }
}
private val dataModule = module {
    single {
        RealRepository(
            context = get(),
            songRepository = get(),
            albumRepository = get(),
            artistRepository = get(),
            genreRepository = get(),
            lastAddedRepository = get(),
            playlistRepository = get(),
            searchRepository = get(),
            topPlayedRepository = get(),
            roomRepository = get()
        )
    } bind Repository::class

    single {
        RealSongRepository(get())
    } bind SongRepository::class

    single {
        RealGenreRepository(get(), get())
    } bind GenreRepository::class

    single {
        RealAlbumRepository(get())
    } bind AlbumRepository::class

    single {
        RealArtistRepository(get(), get())
    } bind ArtistRepository::class

    single {
        RealPlaylistRepository(get())
    } bind PlaylistRepository::class

    single {
        RealTopPlayedRepository(get(), get(), get(), get())
    } bind TopPlayedRepository::class

    single {
        RealLastAddedRepository(
            songRepository = get(),
            albumRepository = get(),
            artistRepository = get()
        )
    } bind LastAddedRepository::class

    single {
        RealSearchRepository(
            songRepository = get(),
            albumRepository = get(),
            artistRepository = get(),
            roomRepository = get(),
            genreRepository = get()
        )
    }
}

private val viewModules = module {

    viewModel {
        LibraryViewModel(get())
    }

    viewModel { (albumId: Long) ->
        AlbumDetailsViewModel(repository = get(), albumId = albumId)
    }

    viewModel { (artistId: Long?, artistName: String?) ->
        ArtistDetailsViewModel(realRepository = get(), artistId = artistId, artistName = artistName)
    }

    viewModel { (playlistId: Long) ->
        PlaylistDetailsViewModel(realRepository = get(), playlistId = playlistId)
    }

    viewModel { (genre: Genre) ->
        GenreDetailsViewModel(realRepository = get(), genre = genre)
    }
}

val appModules = listOf(mainModule, dataModule, autoModule, viewModules, roomModule)
