package com.bignerdranch.android.criminalintent;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;

import java.io.File;
import java.util.List;
import java.util.UUID;

public class CrimePagerActivity extends AppCompatActivity
        implements CrimeFragment.Callbacks {
    private static final String EXTRA_CRIME_ID =
            "com.bignerdranch.android.criminalintent.crime_id";
    private static final int REQUEST_PHOTO = 2;

    private ViewPager mViewPager;
    private List<Crime> mCrimes;
    private ViewPager.OnPageChangeListener mOnPageChangeListener;
    private FloatingActionButton mPhotoButton;

    public static Intent newIntent(Context packageContext, UUID crimeId) {
        Intent intent = new Intent(packageContext, CrimePagerActivity.class);
        intent.putExtra(EXTRA_CRIME_ID, crimeId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crime_pager);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        UUID crimeId = (UUID) getIntent()
                .getSerializableExtra(EXTRA_CRIME_ID);

        mViewPager = (ViewPager) findViewById(R.id.activity_crime_pager_view_pager);

        mCrimes = CrimeLab.get(this).getCrimes();
        FragmentManager fragmentManager = getSupportFragmentManager();
        mViewPager.setAdapter(new FragmentStatePagerAdapter(fragmentManager) {

            @Override
            public Fragment getItem(int position) {
                Crime crime = mCrimes.get(position);
                return CrimeFragment.newInstance(crime.getId());
            }

            @Override
            public int getCount() {
                return mCrimes.size();
            }
        });

        mPhotoButton = (FloatingActionButton) findViewById(R.id.fab);

        mOnPageChangeListener = new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                Crime crime = mCrimes.get(position);
                updateCurrentCrime(crime);
                final Intent captureImage = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                File photoFile = CrimeLab.get(CrimePagerActivity.this).getPhotoFile(crime);

                boolean canTakePhoto = photoFile != null && captureImage.resolveActivity(getPackageManager()) != null;
//                mPhotoButton.setEnabled(canTakePhoto);

                if (canTakePhoto) {
                    Uri uri = Uri.fromFile(photoFile);
                    captureImage.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                }

                mPhotoButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivityForResult(captureImage, REQUEST_PHOTO);
                    }
                });

            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        };
        mViewPager.addOnPageChangeListener(mOnPageChangeListener);

        for (int i = 0; i < mCrimes.size(); i++) {
            if (mCrimes.get(i).getId().equals(crimeId)) {
                mViewPager.setCurrentItem(i);
                break;
            }
        }
    }

    private void updateCurrentCrime(Crime crime) {
        if (crime.getTitle() != null) {
            setTitle(crime.getTitle());
            CollapsingToolbarLayout collapsingToolbar =
                    (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
            collapsingToolbar.setTitle(crime.getTitle());
        }
        ImageView imageView = (ImageView) findViewById(R.id.backdrop);
        if (imageView != null) {
            File photoFile = CrimeLab.get(CrimePagerActivity.this).getPhotoFile(crime);
            if (photoFile == null || !photoFile.exists()) {
                imageView.setImageDrawable(null);
            } else {
                Bitmap bitmap = PictureUtils.getScaledBitmap(
                        photoFile.getPath(), CrimePagerActivity.this);
                imageView.setImageBitmap(bitmap);
            }
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        mPhotoFile = CrimeLab.get(this).getPhotoFile();
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        mViewPager.removeOnPageChangeListener(mOnPageChangeListener);
    }

    @Override
    public void onCrimeUpdated(Crime crime) {
        Crime currentCrime = mCrimes.get(mViewPager.getCurrentItem());
        if (currentCrime == null || crime == null || !currentCrime.getId().equals(crime.getId())) {
            return;
        }

        updateCurrentCrime(crime);
    }
}
