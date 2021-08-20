package com.company.altasnotas.fragments.player;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.company.altasnotas.MainActivity;
import com.company.altasnotas.R;
import com.company.altasnotas.adapters.ChoosePlaylistAdapter;
import com.company.altasnotas.databinding.BottomPlaylistSongPlayerSettingsLayoutBinding;
import com.company.altasnotas.databinding.FragmentPlayerBinding;
import com.company.altasnotas.fragments.favorites.FavoritesFragment;
import com.company.altasnotas.fragments.playlists.CurrentPlaylistFragment;
import com.company.altasnotas.models.Playlist;
import com.company.altasnotas.models.Song;
import com.company.altasnotas.services.BackgroundService;
import com.company.altasnotas.viewmodels.fragments.player.PlayerFragmentViewModel;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.Util;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;


public class PlayerFragment extends Fragment {
    public static ImageButton fav_btn;
    private Button settings_btn;
    private Playlist playlist;
    int position;
    private ImageView song_img;
    private ExoListener exoListener;
    private TextView title, author;

    private DatabaseReference database_ref;
    private FirebaseAuth mAuth;

    public static PlayerView playerView;
    public static BackgroundService mService;
    public static boolean mBound = false;
    private Intent intent;
    private final Long seekedTo;
    private final Boolean isReOpen;
    private Palette palette;
    LinearLayout player_full_box;
    Integer isFav;
    Integer state;
    Boolean ready;
    public final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            BackgroundService.LocalBinder binder = (BackgroundService.LocalBinder) iBinder;
            mService = binder.getService();
            mBound = true;
        if(mService.position==null){
         mService.destroyNotif();
        }else{
            initializePlayer();
            initializeMiniPlayer();
        }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBound = false;
        }
    };



    private Boolean shouldPlay=null;

    private BottomSheetDialog bottomSheetDialog;
    private BottomSheetDialog choosePlaylistDialog;
    private BottomSheetDialog songInPlaylistDialog;
    private PlayerFragmentViewModel viewModel;


    //Mini Player
    public static FragmentPlayerBinding binding;
    public PlayerFragment(Playlist playlist, int position, long seekedTo, Boolean isReOpen,Integer state, Boolean ready, Integer isFav) {
        this.playlist = null;
        this.playlist = playlist;
        this.position = position;
        this.seekedTo = seekedTo;
        this.isReOpen=isReOpen;
        this.state=state;
        this.ready=ready;
        this.isFav=isFav;
        //We are sending playlist to this player and let it play all of it
       /*  isFav
        0 - Playlist
        -1 - Album
        1 - Fav
         */
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentPlayerBinding.inflate(inflater,container,false);
        View view = binding.getRoot();

        title = binding.playerView.findViewById(R.id.player_song_title);
        author = binding.playerView.findViewById(R.id.player_song_author);
        song_img = binding.playerView.findViewById(R.id.player_song_img);
        playerView = binding.playerView.findViewById(R.id.player_view);
        playerView.setBackgroundColor(Color.TRANSPARENT);
        player_full_box = binding.playerView.findViewById(R.id.player_full_box);
        viewModel = new ViewModelProvider(requireActivity()).get(PlayerFragmentViewModel.class);
        MainActivity.activityMainBinding.mainActivityBox.setBackgroundColor(Color.WHITE);
        fav_btn = binding.playerView.findViewById(R.id.player_song_fav_btn);
        settings_btn = binding.playerView.findViewById(R.id.player_song_options_btn);
        database_ref = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        
        //Mini Player
        MainActivity.activityMainBinding.slidingLayout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
                if(newState == SlidingUpPanelLayout.PanelState.EXPANDED){
                    binding.miniIncluded.getRoot().setVisibility(View.GONE);
                }

                if(newState== SlidingUpPanelLayout.PanelState.HIDDEN){
                    binding.miniIncluded.getRoot().setVisibility(View.VISIBLE);
                }

                if(newState== SlidingUpPanelLayout.PanelState.COLLAPSED){
                    binding.miniIncluded.getRoot().setVisibility(View.VISIBLE);
                }
            }
        });


        binding.miniIncluded.miniPlayerTitle.setSelected(true);

        binding.miniIncluded.miniPlayerDescription.setSelected(true);


                intent = new Intent(getActivity(), BackgroundService.class);
                intent.putExtra("playlist", playlist);
                intent.putExtra("pos", position);
                intent.putExtra("path", playlist.getSongs().get(position).getPath());
                intent.putExtra("playlistTitle", playlist.getTitle());
                intent.putExtra("desc", playlist.getDescription());
                intent.putExtra("ms", seekedTo);
                intent.putExtra("isFav",isFav);
                intent.putParcelableArrayListExtra("songs", playlist.getSongs());

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(getActivity(), intent);
                } else {
                    getActivity().startService(intent);
                }

        setUI();

        //Loading fav btn state
        database_ref.child("fav_music")
                .child(mAuth.getCurrentUser().getUid())
                .orderByKey()
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot != null) {
                            Song mySong = playlist.getSongs().get(position);

                            for (DataSnapshot ds : snapshot.getChildren()) {

                                if (
                                        ds.child("album").getValue().equals(mySong.getAlbum())
                                                &&
                                                ds.child("author").getValue().equals(mySong.getAuthor())
                                ) {
                                    //Same album and Author now we check song title
                                    database_ref
                                            .child("music")
                                            .child("albums")
                                            .child(mySong.getAuthor())
                                            .child(mySong.getAlbum())
                                            .addListenerForSingleValueEvent(new ValueEventListener() {

                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot snap) {

                                                    for (DataSnapshot s : snap.child("songs").getChildren()) {

                                                        if (
                                                                s.child("order").getValue().toString().trim().equals(ds.child("numberInAlbum").getValue().toString().trim())
                                                                        &&
                                                                        s.child("title").getValue().equals(mySong.getTitle())
                                                        ) {
                                                            //We found a song in Album and We need to set icon
                                                            fav_btn.setImageResource(R.drawable.ic_heart_full);
                                                            fav_btn.getDrawable().setTint(ContextCompat.getColor(getActivity(), R.color.project_light_orange));

                                                        }
                                                    }

                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError error) {

                                                }
                                            });
                                }
                            }

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });


        fav_btn.setOnClickListener(v -> {

            if (fav_btn.getDrawable().getConstantState().equals(fav_btn.getContext().getDrawable(R.drawable.ic_heart_empty).getConstantState())) {

                viewModel.addToFav( getActivity(),database_ref,mAuth,playlist,position,fav_btn,binding.miniIncluded.miniPlayerFavBtn, CurrentPlaylistFragment.adapter);
            } else {
                viewModel.removeFromFav( getActivity(), database_ref,mAuth,playlist,position,fav_btn,binding.miniIncluded.miniPlayerFavBtn, CurrentPlaylistFragment.adapter);
            }
        });

        settings_btn.setOnClickListener(v -> {
           if(isFav==0){
                openSongInPlaylistsSettingsDialog();
           }else{
               openSettingsDialog();
           }
        });


        binding.miniIncluded.miniPlayerDismissBtn.setOnClickListener(v->{
            dismissPlayer();
        });

        binding.miniIncluded.miniPlayerFavBtn.setOnClickListener(v -> {

            if (binding.miniIncluded.miniPlayerFavBtn.getDrawable().getConstantState().equals(binding.miniIncluded.miniPlayerFavBtn.getContext().getDrawable(R.drawable.ic_heart_empty).getConstantState())) {

                viewModel.addToFav( getActivity(),database_ref,mAuth,playlist,position,fav_btn,binding.miniIncluded.miniPlayerFavBtn, CurrentPlaylistFragment.adapter);
            } else {
                viewModel.removeFromFav( getActivity(), database_ref,mAuth,playlist,position,fav_btn,binding.miniIncluded.miniPlayerFavBtn, CurrentPlaylistFragment.adapter);
            }
        });
        return view;
    }

    public void dismissPlayer() {
        MainActivity.activityMainBinding.slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);

        if(playerView!=null) {
            mService.destroyNotif();
        }

        MainActivity.currentSongTitle.setValue("");
        MainActivity.currentSongAlbum.setValue("");
        MainActivity.currentSongAuthor.setValue("");

        Fragment currentFragment = getActivity().getSupportFragmentManager().findFragmentById(R.id.main_fragment_container);
        if (currentFragment instanceof CurrentPlaylistFragment) {
           if(CurrentPlaylistFragment.adapter!=null){
               CurrentPlaylistFragment.adapter.notifyDataSetChanged();
           }
        }
        if (currentFragment instanceof FavoritesFragment) {
            FavoritesFragment favoritesFragment = (FavoritesFragment) currentFragment;
           if(favoritesFragment.viewModel!=null){
               if(favoritesFragment.viewModel.adapter!=null){
                   favoritesFragment.viewModel.adapter.notifyDataSetChanged();
               }
           }
        }
        //Very important
        if (mBound) {
            getActivity().unbindService(mConnection);
            mBound=false;
        }


        Log.d("PlayerFragment", "Player dismissed!");
    }

    public void initializeMiniPlayer() {
        if (mBound) {
            if(mService.position!=null){
                SimpleExoPlayer player = mService.getPlayerInstance();
                exoListener = new ExoListener(player);
                player.addListener(exoListener);
                binding.miniIncluded.miniPlayerView.setKeepContentOnPlayerReset(true);
                binding.miniIncluded.miniPlayerView.setPlayer(player);
                binding.miniIncluded.miniPlayerView.setUseController(true);
                binding.miniIncluded.miniPlayerView.showController();
                binding.miniIncluded.miniPlayerView.setControllerShowTimeoutMs(0);
                binding.miniIncluded.miniPlayerView.setCameraDistance(0);
                binding.miniIncluded.miniPlayerView.setControllerAutoShow(true);
                binding.miniIncluded.miniPlayerView.setDrawingCacheBackgroundColor(Color.TRANSPARENT);
                binding.miniIncluded.miniPlayerView.setShutterBackgroundColor(Color.TRANSPARENT);
                binding.miniIncluded.miniPlayerView.setControllerHideOnTouch(false);
            }
        }
    }
    private void openSongInPlaylistsSettingsDialog() {
        songInPlaylistDialog = new BottomSheetDialog(getContext());
        songInPlaylistDialog.setContentView(R.layout.bottom_playlist_song_player_settings_layout);

        LinearLayout showAlbum = songInPlaylistDialog.findViewById(R.id.bottom_settings_album_box);
        LinearLayout showPlaylist= songInPlaylistDialog.findViewById(R.id.bottom_settings_show_playlist_box);
        LinearLayout share = songInPlaylistDialog.findViewById(R.id.bottom_settings_share_box);
        LinearLayout dismissDialog = songInPlaylistDialog.findViewById(R.id.bottom_settings_dismiss_box);

        showAlbum.setOnClickListener(v -> {
            //Shows album
            //Download playlist
            Playlist x = new Playlist();
            if (mAuth.getCurrentUser() != null) {

                database_ref.child("music").child("albums").child(playlist.getSongs().get(position).getAuthor()).child(playlist.getSongs().get(position).getAlbum()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot != null) {
                            x.setImage_id(snapshot.child("image_id").getValue().toString());
                            x.setYear(snapshot.child("year").getValue().toString());
                            x.setTitle(snapshot.child("title").getValue().toString());
                            x.setDescription(snapshot.child("description").getValue().toString());
                            x.setDir_title(playlist.getSongs().get(position).getAlbum());
                            x.setDir_desc(playlist.getSongs().get(position).getAuthor());
                            songInPlaylistDialog.dismiss();
                            getActivity().getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_left, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out_left).replace(R.id.main_fragment_container, new CurrentPlaylistFragment(playlist.getSongs().get(position).getAuthor(), playlist.getSongs().get(position).getAlbum(), x, 1)).addToBackStack("null").commit();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        android.util.Log.d(MainActivity.FIREBASE,"Error: " + error.getMessage());
                    }

                });


            }
        });

        showPlaylist.setOnClickListener(v->{
          for(int i=0; i<getActivity().getSupportFragmentManager().getBackStackEntryCount(); i++){
              getActivity().getSupportFragmentManager().popBackStack();
          }
            getActivity().getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_left, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out_left).replace(R.id.main_fragment_container, new CurrentPlaylistFragment(playlist.getTitle(), "",playlist, 0)).addToBackStack("null").commit();

            songInPlaylistDialog.dismiss();
        });

        share.setOnClickListener(v -> {
            share();
            songInPlaylistDialog.dismiss();
        });
        dismissDialog.setOnClickListener(v -> songInPlaylistDialog.dismiss());

        songInPlaylistDialog.show();
    }

    private void openSettingsDialog() {
        bottomSheetDialog = new BottomSheetDialog(getContext());
        bottomSheetDialog.setContentView(R.layout.bottom_song_settings_layout);

        LinearLayout showAlbum = bottomSheetDialog.findViewById(R.id.bottom_settings_album_box);
        LinearLayout addToPlaylist = bottomSheetDialog.findViewById(R.id.bottom_settings_playlists_box);
        LinearLayout share = bottomSheetDialog.findViewById(R.id.bottom_settings_share_box);
        LinearLayout dismissDialog = bottomSheetDialog.findViewById(R.id.bottom_settings_dismiss_box);

        showAlbum.setOnClickListener(v -> {
            //Shows album
            //Download playlist
            Playlist x = new Playlist();
            if (mAuth.getCurrentUser() != null) {

                database_ref.child("music").child("albums").child(playlist.getSongs().get(position).getAuthor()).child(playlist.getSongs().get(position).getAlbum()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot != null) {
                            x.setImage_id(snapshot.child("image_id").getValue().toString());
                            x.setYear(snapshot.child("year").getValue().toString());
                            x.setTitle(snapshot.child("title").getValue().toString());
                            x.setDescription(snapshot.child("description").getValue().toString());
                            x.setDir_title(playlist.getSongs().get(position).getAlbum());
                            x.setDir_desc(playlist.getSongs().get(position).getAuthor());
                            bottomSheetDialog.dismiss();
                            getActivity().getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_left, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out_left).replace(R.id.main_fragment_container, new CurrentPlaylistFragment(playlist.getSongs().get(position).getAuthor(), playlist.getSongs().get(position).getAlbum(), x, 1)).addToBackStack("null").commit();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        android.util.Log.d(MainActivity.FIREBASE,"Error: " + error.getMessage());
                    }

                });


            }
        });

        addToPlaylist.setOnClickListener(v -> {
            //Add to playlist
            addToPlaylist();
            bottomSheetDialog.dismiss();
        });

        share.setOnClickListener(v -> {
            share();
            bottomSheetDialog.dismiss();
        });
        dismissDialog.setOnClickListener(v -> bottomSheetDialog.dismiss());

        bottomSheetDialog.show();
    }

    private void addToPlaylist() {
        choosePlaylistDialog = new BottomSheetDialog(getContext());
        choosePlaylistDialog.setContentView(R.layout.choose_playlist_dialog);
        TextView chooseState = choosePlaylistDialog.findViewById(R.id.choose_playlist_recycler_view_state);
        RecyclerView chooseRecyclerView = choosePlaylistDialog.findViewById(R.id.choose_playlist_recycler_view);
        ArrayList<String> playlists_titles = new ArrayList<>();
        ArrayList<String> playlists_keys = new ArrayList<>();

        database_ref.child("music").child("playlists").child(mAuth.getCurrentUser().getUid()).orderByKey().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                int x = 0;
                for (DataSnapshot playlistSnapshot : snapshot.getChildren()) {
                    x++;

                    playlists_titles.add(playlistSnapshot.child("title").getValue().toString());
                    playlists_keys.add(playlistSnapshot.getKey());


                }

                if(snapshot.getChildrenCount()!=0) {
                    if (x == snapshot.getChildrenCount()) {


                        ChoosePlaylistAdapter choosePlaylistAdapter = new ChoosePlaylistAdapter((MainActivity) requireActivity(), choosePlaylistDialog, playlist.getSongs().get(position), playlists_titles, playlists_keys);
                        chooseRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
                        choosePlaylistAdapter.notifyDataSetChanged();
                        chooseRecyclerView.setAdapter(choosePlaylistAdapter);
                    }

                    chooseState.setVisibility(View.GONE);
                    chooseRecyclerView.setVisibility(View.VISIBLE);
                }else{
                        chooseState.setVisibility(View.VISIBLE);
                        chooseRecyclerView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        choosePlaylistDialog.show();
    }

    private void share() {

        // The application exists
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(android.content.Intent.EXTRA_TITLE, "Altas Notas");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "My favorite Song is \"" + playlist.getSongs().get(position).getTitle() + "\" from \"" + playlist.getSongs().get(position).getAuthor() + "\".\nListen this on \"Altas Notas\".\nExternal Link: [ " + playlist.getSongs().get(position).getPath() + " ]");
        startActivity(Intent.createChooser(shareIntent, "Share using"));
        getContext().startActivity(shareIntent);

    }

    public void initializePlayer() {
        if (mBound) {
   if(mService.position!=null){
       SimpleExoPlayer player = mService.getPlayerInstance();
       exoListener = new ExoListener(player);
       player.addListener(exoListener);
       playerView.setKeepContentOnPlayerReset(true);
       playerView.setPlayer(player);
       playerView.setUseController(true);
       playerView.showController();
       playerView.setControllerShowTimeoutMs(0);
       playerView.setCameraDistance(0);
       playerView.setControllerAutoShow(true);

       System.out.println(isReOpen+","+ shouldPlay);
       if(state==null || ready==null){
           if(isReOpen)
           {
               //By this When Notification is Open and ExoPlayer is Paused. It remains that way.
               player.setPlayWhenReady(player.getPlayWhenReady() && player.getPlaybackState() == Player.STATE_READY);
           }
           else
           {
               if(shouldPlay!=null){
                   player.setPlayWhenReady(shouldPlay);
               }else{
                   player.setPlayWhenReady(true);
               }
           }


       }else{

           if(!(ready && state == Player.STATE_READY)){
               player.setPlayWhenReady(false);
           }else{
               player.setPlayWhenReady(true);
           }
       }
   }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
       requireActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
  /*
        Fragment currentFragment = getActivity().getSupportFragmentManager().findFragmentById(R.id.main_mini_player_container);
        if (currentFragment instanceof MiniPlayerFragment) {
            MiniPlayerFragment miniPlayerFragment = (MiniPlayerFragment) currentFragment;
            requireActivity().bindService(miniPlayerFragment.intent, mConnection, Context.BIND_AUTO_CREATE);
        }else{

        }
   */
    }

    public void setUI() {
        setMiniUI();
        if(getActivity()!=null) {
            Fragment currentFragment = getActivity().getSupportFragmentManager().findFragmentById(R.id.sliding_layout_frag);
            if (currentFragment instanceof PlayerFragment) {
                Glide.with(getActivity()).load(playlist.getSongs().get(position).getImage_url()).error(R.drawable.img_not_found).into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        if(resource!=null) {
                            song_img.setImageDrawable(resource);
                            Bitmap b = drawableToBitmap(resource);
                            palette = Palette.from(b).generate();
                          viewModel.setUpInfoBackgroundColor(getActivity(), player_full_box, palette);
                            if (fav_btn.getDrawable().getConstantState().equals(fav_btn.getContext().getDrawable(R.drawable.ic_heart_empty).getConstantState())) {
                                fav_btn.getDrawable().setTint(Color.WHITE);
                            }else{
                                fav_btn.getDrawable().setTint(ContextCompat.getColor(getActivity(), R.color.project_light_orange));
                              }
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });

                database_ref = FirebaseDatabase.getInstance().getReference();
                database_ref.child("music").child("albums").child(playlist.getSongs().get(position).getAuthor()).child(playlist.getSongs().get(position).getAlbum()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        //We add extra space because This Light font have small spacing between words.
                     String local_title = playlist.getSongs().get(position).getTitle();
                     title.setTag(" ");
                     String space = (String) title.getTag();
                        title.setText(local_title.replace(space, (space += " ")));
                        title.setTag(space);


                        author.setText(snapshot.child("description").getValue().toString());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });

                //Loading fav btn state
                if(mAuth.getCurrentUser()!=null){
                    database_ref.child("fav_music")
                            .child(mAuth.getCurrentUser().getUid())
                            .orderByKey()
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {

                                    if (snapshot != null) {
                                        Song mySong = playlist.getSongs().get(position);

                                        for (DataSnapshot ds : snapshot.getChildren()) {

                                            if (
                                                    ds.child("album").getValue().equals(mySong.getAlbum())
                                                            &&
                                                            ds.child("author").getValue().equals(mySong.getAuthor())
                                            ) {
                                                //Same album and Author now we check song title
                                                database_ref
                                                        .child("music")
                                                        .child("albums")
                                                        .child(mySong.getAuthor())
                                                        .child(mySong.getAlbum())
                                                        .addListenerForSingleValueEvent(new ValueEventListener() {

                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot snap) {

                                                                for (DataSnapshot s : snap.child("songs").getChildren()) {

                                                                    if (
                                                                            s.child("order").getValue().toString().trim().equals(ds.child("numberInAlbum").getValue().toString().trim())
                                                                                    &&
                                                                                    s.child("title").getValue().equals(mySong.getTitle())
                                                                    ) {
                                                                        //We found a song in Album and We need to set icon
                                                                        fav_btn.setImageResource(R.drawable.ic_heart_full);
                                                                        fav_btn.getDrawable().setTint(ContextCompat.getColor(getActivity(),R.color.project_light_orange));
                                                                    }
                                                                }

                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError error) {

                                                            }
                                                        });
                                            }
                                        }

                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                }
            }
        }else{
            Log.d("Activity", "Activity is null");
        }
    }

    public  void setMiniUI() {
        if(getActivity()!=null) {
            Fragment currentFragment = getActivity().getSupportFragmentManager().findFragmentById(R.id.sliding_layout_frag);
            if (currentFragment instanceof PlayerFragment) {
                Glide.with(getActivity()).load(playlist.getSongs().get(position).getImage_url()).error(R.drawable.img_not_found).into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        if(resource!=null) {
                            binding.miniIncluded.miniPlayerImg.setImageDrawable(resource);
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });

                database_ref = FirebaseDatabase.getInstance().getReference();
                database_ref.child("music").child("albums").child(playlist.getSongs().get(position).getAuthor()).child(playlist.getSongs().get(position).getAlbum()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        binding.miniIncluded.miniPlayerTitle.setText(playlist.getSongs().get(position).getTitle());
                        binding.miniIncluded.miniPlayerDescription.setText(snapshot.child("description").getValue().toString());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });

            }
        }


        //Loading fav btn state
        if(mAuth.getCurrentUser()!=null){
            database_ref.child("fav_music")
                    .child(mAuth.getCurrentUser().getUid())
                    .orderByKey()
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {

                            if (snapshot != null) {
                                Song mySong = playlist.getSongs().get(position);

                                for (DataSnapshot ds : snapshot.getChildren()) {

                                    if (
                                            ds.child("album").getValue().equals(mySong.getAlbum())
                                                    &&
                                                    ds.child("author").getValue().equals(mySong.getAuthor())
                                    ) {
                                        //Same album and Author now we check song title
                                        database_ref
                                                .child("music")
                                                .child("albums")
                                                .child(mySong.getAuthor())
                                                .child(mySong.getAlbum())
                                                .addListenerForSingleValueEvent(new ValueEventListener() {

                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot snap) {

                                                        for (DataSnapshot s : snap.child("songs").getChildren()) {

                                                            if (
                                                                    s.child("order").getValue().toString().trim().equals(ds.child("numberInAlbum").getValue().toString().trim())
                                                                            &&
                                                                            s.child("title").getValue().equals(mySong.getTitle())
                                                            ) {
                                                                //We found a song in Album and We need to set icon
                                                                binding.miniIncluded.miniPlayerFavBtn.setImageResource(R.drawable.ic_heart_full);
                                                                binding.miniIncluded.miniPlayerFavBtn.getDrawable().setTint(ContextCompat.getColor(getActivity(),R.color.project_dark_velvet));
                                                            }
                                                        }

                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError error) {

                                                    }
                                                });
                                    }
                                }

                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
        }


        if (binding.miniIncluded.miniPlayerFavBtn.getDrawable().getConstantState().equals(binding.miniIncluded.miniPlayerFavBtn.getContext().getDrawable(R.drawable.ic_heart_empty).getConstantState())) {
            binding.miniIncluded.miniPlayerFavBtn.getDrawable().setTint(Color.BLACK);
        }else{
            binding.miniIncluded.miniPlayerFavBtn.getDrawable().setTint(ContextCompat.getColor(getActivity(), R.color.project_dark_velvet));
        }
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public void setSongState(boolean b) {
        shouldPlay =b;
    }
    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        initializePlayer();
        initializeMiniPlayer();
    }

    public  class ExoListener implements Player.Listener {
        SimpleExoPlayer player;

        public ExoListener(SimpleExoPlayer player) {
            this.player = player;
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            mService.setPosition(player.getCurrentWindowIndex());
            position = player.getCurrentWindowIndex();
            playerView.setPlayer(player);
            state= player.getPlaybackState();
            ready = player.getPlayWhenReady();
          if(CurrentPlaylistFragment.adapter!=null){
              CurrentPlaylistFragment.adapter.notifyDataSetChanged();
          }

            Log.d("Exo","playbackState = " + playbackState + " playWhenReady = " + playWhenReady );
            switch (playbackState) {
                case Player.STATE_IDLE:
                    // free
                    break;
                case Player.STATE_BUFFERING:
                    // Buffer
                    break;
                case Player.STATE_READY:
                    // Get ready
                    break;
                case Player.STATE_ENDED:
                    // End
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            // Report errors
            switch (error.type) {
                case ExoPlaybackException.TYPE_SOURCE:
                    // Error loading resources
                    break;
                case ExoPlaybackException.TYPE_RENDERER:
                    // Errors in rendering
                    break;
                case ExoPlaybackException.TYPE_UNEXPECTED:
                    // unexpected error
                    break;
            }
        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
                 position = player.getCurrentWindowIndex();
                MainActivity.currentSongTitle.setValue(playlist.getSongs().get(position).getTitle());
                MainActivity.currentSongAlbum.setValue(playlist.getTitle());
                MainActivity.currentSongAuthor.setValue(playlist.getDescription());
              if(CurrentPlaylistFragment.adapter!=null){
              CurrentPlaylistFragment.adapter.notifyDataSetChanged();
          }

            setUI();
            fav_btn.setImageResource(R.drawable.ic_heart_empty);
            mService.setPosition(position);
        }
    }
    public  PlayerFragment getPlayerFragment(){
        return  this;
    }
}

