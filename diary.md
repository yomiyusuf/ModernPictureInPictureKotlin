# Development Diary - Picture in Picture Kotlin Challenge
## 1. Getting Started - Missing Sample Directory
The first thing that tripped me up was the repo link. It didn’t work.
After cloning Google’s sample repository I realised the PictureInPictureKotlin folder was deleted from main around twoSo I checked the git history and checked out the last commit that still had it. 
That became my starting point. 
First Impressions of the Codebase
I reviewd the code to see what it did, but it was more of a quick demo app.
It only supported API 31+, the timer was stuck inside the activity, and there were only a few instrumentation tests.
A few notes I jotted down:
- minSdkVersion 31 was way too high
- timer logic was inside the activity lifecycle, not reusable
- used LiveData everwhere, felt pretty outdated
  So my first goal was to modernise things a bit before adding new features.

## 3. Tooling and Build Setup
Android Studio didn’t like the setup. Java 21 with Gradle 7.5 gave me sync errors.
I upgraded Gradle to 8.5, AGP to 8.1.4, and locked the build to Java 17 which fixed it.
That also made it easier to use newer libs like Hilt and DataStore later.

## 4. Architecture Planning
Before writing code I wrote out a rough plan.
- use a repository for timer state
- switch to StateFlow instead of LiveData
- add Hilt for dependency injection
- maybe persist timer state with DataStore later
  I wanted the project to look like something that could live in a real app, not just a sample.

## 5. Migrating from LiveData to Flow
This was the first actual change.
Flow is just easier to test and more consistent with modern Android.
Replaced:
MutableLiveData<Boolean> -> MutableStateFlow<Boolean>
MutableLiveData<Long> -> MutableStateFlow<Long>
and used stateIn with SharingStarted.WhileSubscribed(5000).
Immediately cleaner and made testing much simpler.

## 6. Adding Dependency Injection
Then I added Hilt.
Created a PictureInPictureApplication with @HiltAndroidApp, annotated both activities, and made a small TimeProvider. At first it looked a bit over-engineered but I knew it’d make testing time behaviour much easier later.

## 7. Setting Up Unit Testing
Before adding any new logic I wanted tests working.
The project had none for ViewModels.
Added:
- kotlinx-coroutines-test
- app.cash.turbine
- MockK and Truth
  Then I wrote a few small tests for MainViewModel using a fake TimeProvider.
  It was fast and reliable compared to old LiveData tests.

## 8. Building the Timer Repository (Task 3)
The biggest job was to make the timer keep running between screens.
I built a TimerRepository with Hilt @Singleton scope so both activities could share it.
MainViewModel became a lightweight wrapper that just delegates to the repository.
Same with MovieViewModel.
I also removed all the finish() calls between activities so they don’t destroy themselves.
Suddenly the timer just kept ticking, even when switching screens.
Pretty satisfying moment, to be honest.

## 9. Testing the New Architecture
After that refactor, old tests broke (expected).
I rewrote them around the new responsibilities:
- TimerRepositoryTest checked start, pause, resume, and reset.
- MainViewModelTest just verified delegation using MockK.
  The tests became much shorter and more reliable.
  No more hanging coroutines or random delays.

## 10. Adding Legacy Support (Task 1)
Now that it worked nicely I dropped the min SDK to 21.
That broke Picture in Picture calls on old devices obviously.
I made a small helper called PipCompatibilityManager to safely wrap PiP code.
Devices under API 26 just skip PiP features and show a short info text.
Used different layouts:
- res/layout/main_activity.xml for normal
- res/layout-v26/main_activity.xml for PiP enabled
  So old devices still look clean without dead buttons.

## 11. Enhanced PiP for Android 12+
Once base support was stable I added small extras for Android 12+.
 - setAutoEnterEnabled(true) and setSourceRectHint() make the transition smoother when using gestures.
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
builder.setAutoEnterEnabled(true)
builder.setSourceRectHint(sourceRect)
}
It’s a small detail but feels more polished on newer phones.