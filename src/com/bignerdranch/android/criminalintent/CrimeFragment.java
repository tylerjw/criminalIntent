package com.bignerdranch.android.criminalintent;

import java.util.Date;
import java.util.UUID;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NavUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

public class CrimeFragment extends Fragment {
	private static final String TAG = "CrimeFragment";
	
	private Crime mCrime;
	private EditText mTitleField;
	private Button mDateButton;
	private CheckBox mSolvedCheckBox;
	private ImageView mPhotoView;
	private ImageButton mPhotoButton;
	private Button mSuspectButton;
	private Callbacks mCallbacks;
	
	public static final String EXTRA_CRIME_ID = "com.bignerdranch.android.crime_id";
	private static final String DIALOG_DATE = "date";
	private static final String DIALOG_IMAGE = "image";
	private static final int REQUEST_DATE = 0;
	private static final int REQUEST_PHOTO = 1;
	private static final int REQUEST_CONTACT = 2;
	
	/**
	 * Required interface for hosting activities
	 */
	public interface Callbacks {
		void onCrimeUpdated(Crime crime);
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mCallbacks = (Callbacks)activity;
	}
	
	@Override
	public void onDetach() {
		super.onDetach();
		mCallbacks = null;
	}
	
	public static CrimeFragment newInstance(UUID crimeId) {
		Bundle args = new Bundle();
		args.putSerializable(EXTRA_CRIME_ID, crimeId);
		CrimeFragment fragment = new CrimeFragment();
		fragment.setArguments(args);
		return fragment;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true); // for the up button
		
		UUID crimeId = (UUID)getArguments().getSerializable(EXTRA_CRIME_ID);
		mCrime = CrimeLab.get(getActivity()).getCrime(crimeId);
	}
	
	@TargetApi(11)
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup parent,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_crime, parent, false);
		
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			if(NavUtils.getParentActivityName(getActivity()) != null) {
				getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
			}
		}
		
		mTitleField = (EditText)v.findViewById(R.id.crime_title);
		mTitleField.setText(mCrime.getTitle());
		mTitleField.addTextChangedListener(new TextWatcher() {
			public void onTextChanged( CharSequence c, int start,
					int before, int count) {
				mCrime.setTitle(c.toString());
				mCallbacks.onCrimeUpdated(mCrime);
				// getActivity().setTitle(mCrime.getTitle()); // TODO: test what this does
			}
			
			public void beforeTextChanged( CharSequence c, int start,
					int count, int after) {
				// This space intentionally left blank
			}
			
			public void afterTextChanged(Editable c) {
				// This one too
			}
		});
		
		mDateButton = (Button)v.findViewById(R.id.crime_date);
		updateDate();
		mDateButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentManager fm = getActivity().getSupportFragmentManager();
				DatePickerFragment dialog = DatePickerFragment.newInstance(mCrime.getDate());
				dialog.setTargetFragment(CrimeFragment.this, REQUEST_DATE);
				dialog.show(fm, DIALOG_DATE);
			}
		});
		
		mSolvedCheckBox = (CheckBox)v.findViewById(R.id.crime_solved);
		mSolvedCheckBox.setChecked(mCrime.isSolved());
		mSolvedCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				// Set the crime's solved property
				mCrime.setSolved(isChecked);
				mCallbacks.onCrimeUpdated(mCrime);
			}
		});
		
		mPhotoButton = (ImageButton)v.findViewById(R.id.crime_imageButton);
		mPhotoButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent i = new Intent(getActivity(), CrimeCameraActivity.class);
				startActivityForResult(i, REQUEST_PHOTO);
				
			}
		});
		// If camera is not available, disable camera functionality
		PackageManager pm = getActivity().getPackageManager();
		if(!pm.hasSystemFeature(PackageManager.FEATURE_CAMERA) && !pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
			mPhotoButton.setEnabled(false);
		}
		
		mPhotoView = (ImageView)v.findViewById(R.id.crime_imageView);
		mPhotoView.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Photo p = mCrime.getPhoto();
				if(p == null)
					return;
				
				FragmentManager fm = getActivity().getSupportFragmentManager();
				String path = getActivity().getFileStreamPath(p.getFilename()).getAbsolutePath();
				ImageFragment.newInstance(path).show(fm,  DIALOG_IMAGE);
			}
		});
		
		Button reportButton = (Button)v.findViewById(R.id.crime_reportButton);
		reportButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_SEND);
				i.setType("text/plain");
				i.putExtra(Intent.EXTRA_TEXT, getCrimeReport());
				i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_subject));
				i = Intent.createChooser(i, getString(R.string.send_report));
				startActivity(i);
			}
		});
		
		mSuspectButton = (Button)v.findViewById(R.id.crime_suspectButton);
		mSuspectButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_PICK,ContactsContract.Contacts.CONTENT_URI);
				startActivityForResult(i, REQUEST_CONTACT);
			}
		});
		
		if(mCrime.getSuspect() != null) {
			mSuspectButton.setText(mCrime.getSuspect());
		}
		
		return v;
	}
	
	private void showPhoto() {
		// (Re)set the image button's image based on our photo
		Photo p = mCrime.getPhoto();
		BitmapDrawable b = null;
		if(p != null) {
			String path = getActivity().getFileStreamPath(p.getFilename()).getAbsolutePath();
			b = PictureUtils.getScaledDrawable(getActivity(), path);
		}
		mPhotoView.setImageDrawable(b);
	}
	
//	@Override
//	public void onStart() {
//		super.onStart();
//		Log.d(TAG, "onStart()");
//		showPhoto(); // does not work when returning from taking a photo
//	}
	
	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "onResume()");
		showPhoto(); // moved to onResume from onStart to show photo when returning form taking a photo
	}
	
	@Override
	public void onStop() {
		super.onStop();
		PictureUtils.cleanImageView(mPhotoView);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.fragment_crime, menu);
		
	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case R.id.menu_item_delete_crime:
				CrimeLab.get(getActivity()).deleteCrime(mCrime);
				// fallthrough ... (return to last parent)
			case android.R.id.home:
				if(NavUtils.getParentActivityName(getActivity()) != null) {
					NavUtils.navigateUpFromSameTask(getActivity());
				}
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode != Activity.RESULT_OK) return;
		switch(resultCode) {
			case REQUEST_DATE:
				Date date = (Date)data.getSerializableExtra(DatePickerFragment.EXTRA_DATE);
				mCrime.setDate(date);
				mCallbacks.onCrimeUpdated(mCrime);
				updateDate();
				break;
			case REQUEST_PHOTO:
				// Create a new Photo object and attach it to the crime
				String filename = data.getStringExtra(CrimeCameraFragment.EXTRA_PHOTO_FILENAME);
				if(filename != null) {
					Log.i(TAG, "filename: " + filename);
					
					Photo p = new Photo(filename);
					mCrime.setPhoto(p);
					mCallbacks.onCrimeUpdated(mCrime);
					showPhoto();
				}
				break;
			case REQUEST_CONTACT:
				Uri contactUri = data.getData();
				
				// Specify which fields you want your query to return
				// values for
				String[] queryFields = new String[] {
						ContactsContract.Contacts.DISPLAY_NAME
				};
				// Perform your query - the contactUri is like a "where"
				// clause here
				Cursor c = getActivity().getContentResolver().query(contactUri, queryFields, null, null, null);
				
				// Double-check that you actually got results
				if(c.getCount() == 0) {
					c.close();
					return;
				}
				
				// Pull out the first column of the first row of data - 
				// that is your suspect's name.
				c.moveToFirst();
				String suspect = c.getString(0);
				mCrime.setSuspect(suspect);
				mCallbacks.onCrimeUpdated(mCrime);
				mSuspectButton.setText(suspect);
				c.close();
				break;
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		CrimeLab.get(getActivity()).saveCrimes();
	}
	
	private void updateDate() {
		mDateButton.setText(mCrime.getDate().toString());
	}
	
	private String getCrimeReport() {
		String solvedString = null;
		if(mCrime.isSolved()) {
			solvedString = getString(R.string.crime_report_solved);
		} else {
			solvedString = getString(R.string.crime_report_unsolved);
		}
		String dateFormat = "EEE, MM dd";
		String dateString = DateFormat.format(dateFormat,  mCrime.getDate()).toString();
		
		String suspect = mCrime.getSuspect();
		if(suspect == null) {
			suspect = getString(R.string.crime_report_no_suspect);
		} else {
			suspect = getString(R.string.crime_report_suspect, suspect);
		}
		
		String report = getString(R.string.crime_report, mCrime.getTitle(), dateString, suspect);
		
		return report;
	}
}
