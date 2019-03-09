package com.restri_tech.Fragments;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.restri_tech.Adapter.app;
import com.restri_tech.DB.Package;
import com.restri_tech.HomeActivity;
import com.restri_tech.Adapter.MyArrayAdapter;
import com.restri_tech.MainActivity;
import com.restri_tech.PrefManager;
import com.restri_tech.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.restri_tech.HomeActivity.droid;
import static com.restri_tech.HomeActivity.droidTarget;
import static com.restri_tech.HomeActivity.sassyDesc;


/**
 * A simple {@link Fragment} subclass.
 */
public class InstallFragment extends Fragment {
    RecyclerView myRecyclerView;
    MyArrayAdapter myArrayAdapter;
    PackageManager packageManager = null;
    List <com.restri_tech.Adapter.app> app;
    List <Package> apps;
    EditText ed;
    FloatingActionButton fab;
    private PrefManager prefManager;

    public InstallFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        app = new ArrayList < > ();

        MyTask myTask = new MyTask();
        myTask.execute();

    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        View v = inflater.inflate(R.layout.fragment_install, container, false);

        fab = v.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List < String > resultList = myArrayAdapter.getSelectedItem();
                for (int i = 0; i < resultList.size(); i++) {
                    Package p = new Package();
                    p.setName(resultList.get(i));
                    p.setFtime(TimeUnit.MINUTES.toNanos(0));
                    p.setRtime(TimeUnit.MINUTES.toNanos(0));
                    HomeActivity.myAppDatabase.myDao().addApps(p);
                }

                getActivity().setTitle("Blocked Apps");
                BlockFragment bf = new BlockFragment();
                FragmentManager fm = getFragmentManager();
                fm.beginTransaction().replace(R.id.container, bf).commit();

            }
        });

        Context context = v.getContext();
        myRecyclerView = v.findViewById(R.id.list);
        myRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        myArrayAdapter = new MyArrayAdapter(packageManager, app);
        myRecyclerView.setAdapter(myArrayAdapter);
        ed = v.findViewById(R.id.editTextSearch);
        ed.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                filter(s.toString());
            }
        });

        final TapTargetSequence sequence = new TapTargetSequence(getActivity())
                .targets(
                        // This tap target will target the back button, we just need to pass its containing toolbar
                        TapTarget.forView(fab, "llll", sassyDesc).id(1).cancelable(false).icon(ContextCompat.getDrawable(context,R.drawable.ic_block)),
                        // Likewise, this tap target will target the search button
                        TapTarget.forView(ed, "simple", sassyDesc).id(2).cancelable(false).icon(ContextCompat.getDrawable(context,R.drawable.ic_action_search)
                ))
                .listener(new TapTargetSequence.Listener() {
                    // This listener will tell us when interesting(tm) events happen in regards
                    // to the sequence
                    @Override
                    public void onSequenceFinish() {

                    }

                    @Override
                    public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {
                        Log.d("TapTargetView", "Clicked on " + lastTarget.id());
                    }

                    @Override
                    public void onSequenceCanceled(TapTarget lastTarget) {
                    }
                });

        prefManager = new PrefManager(context,"iapps");
        if (prefManager.isFirstTimeLaunch()) {
            sequence.start();
            prefManager.setFirstTimeLaunch(false);
        }
        return v;
    }

    private class MyTask extends AsyncTask < String, Void, List < app >> {

        private final ProgressDialog dialog = new ProgressDialog(getActivity());

        @Override
        protected void onPostExecute(List < app > result) {
            super.onPostExecute(result);
            for (int i = 0; i < result.size(); i++)
                for (int k = i; k < result.size(); k++)
                    if (result.get(i).aiGet().loadLabel(packageManager).toString().compareTo(result.get(k).aiGet().loadLabel(packageManager).toString()) > 0) {
                        app a = result.get(i);
                        result.set(i, result.get(k));
                        result.set(k, a);
                    }
            app.addAll(result);
            myArrayAdapter.notifyDataSetChanged();
            ed.setVisibility(View.VISIBLE);
            fab.setVisibility(View.VISIBLE);
            dialog.dismiss();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog.setMessage("Loading...");
            dialog.show();
        }

        @Override
        protected List < app > doInBackground(String...params) {
            List < app > result;

            try {
                packageManager = getContext().getPackageManager();
                List < ApplicationInfo > appList;
                result = new ArrayList < > ();
                appList = checkForLaunchIntent(packageManager.getInstalledApplications(PackageManager.GET_META_DATA));
                apps = HomeActivity.myAppDatabase.myDao().getApps();

                for (ApplicationInfo ai: appList) {
                    int flag = 0;
                    for (Package p: apps)
                        if (p.getName().equals(ai.processName)) {
                            flag = 1;
                            break;
                        }
                    if (flag == 0) {
                        app a = new app();
                        a.aiSet(ai);
                        result.add(a);
                    }
                }
                return result;
            } catch (Throwable t) {
                t.printStackTrace();
            }
            return null;
        }

    }
    private List < ApplicationInfo > checkForLaunchIntent(List < ApplicationInfo > list) {
        ArrayList < ApplicationInfo > applist = new ArrayList < > ();
        for (ApplicationInfo info: list) {
            try {
                if (null != packageManager.getLaunchIntentForPackage(info.packageName))
                    applist.add(info);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return applist;
    }
    private void filter(String text) {
        //new array list that will hold the filtered data
        List < app > filterdNames = new ArrayList < > ();

        //looping through existing elements
        for (app s: app) {
            //if the existing elements contains the search input
            if (s.aiGet().processName.toLowerCase().contains(text.toLowerCase())) {
                //adding the element to filtered list
                filterdNames.add(s);
            }
        }

        //calling a method of the adapter class and passing the filtered list
        myArrayAdapter.filterList(filterdNames);
    }

}