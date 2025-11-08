# Development Diary - PictureInPicture Enhancement Project

## Unavailable repository
The supplied url points to a non-existent directory.
On further review of the git history, I foud he PictureInPictureKotlin directory has been deleted 
from the main branch of the repository about 2 weeks ago.
To proceed with the exercise, I checkout the last commit before the deletion which contains the 
latest version of the directory.

## Project Overview
Working on enhancing the Google PictureInPicture sample to implement three key improvements:
1. Legacy support (API 21+)
2. Comprehensive unit testing
3. Shared timer state across activities

### Current Architecture Assessment
The existing sample demonstrates modern PiP usage but has several limitations:
- **minSdkVersion 31**: Very restrictive, excludes many devices still in use
- **No unit tests for ViewModels**: Only instrumentation tests exist
- **Activity-scoped timer**: Timer resets when navigating between activities
- **LiveData usage**: While functional, Flow would be more future-proof

### Key Technical Decisions

#### LiveData → Flow Migration
After reviewing the codebase, decided to migrate from LiveData to Flow for several reasons:
- **Better testability**: Flow testing is more straightforward and doesn't require AndroidX Test
- **Compose readiness**: Future-proofs the codebase for Compose migration
- **Performance**: StateFlow with `WhileSubscribed()` provides better lifecycle management
- **Consistency**: Aligns with modern reactive programming patterns in Android

#### Architecture Strategy
Planning to implement a Repository pattern with dependency injection:
- **TimerRepository**: Single source of truth for timer state
- **Application-scoped ViewModels**: Share state across activities
- **DataStore integration**: Persist timer state across app restarts
- **Foreground Service**: Enable background timer execution

### Implementation Plan Structure
Breaking down the work into atomic commits:
1. **Foundation First**: Dependencies, architecture setup, Flow migration
2. **Legacy Support**: Gradual API compatibility implementation
3. **Testing Infrastructure**: Comprehensive unit test coverage
4. **Shared State**: Cross-activity state management
5. **Polish**: Performance optimization and documentation

### IDE Configuration Setup
**Implementation**: Added complete IDE integration:
- Created Android Studio run configurations for app and MainActivity
- Configured Gradle settings to use Java 17
- Fixed Gradle wrapper to use stable 7.5 version (was updated to 9.0-milestone causing compatibility issues)
- Added DEVELOPMENT.md with setup instructions

### Version Compatibility Resolution
**Issue**: Android Studio detected Java/Gradle version incompatibility - Java 21 with Gradle 7.5 caused sync errors.

**Solution**: Updated build system for Java 21 compatibility:
- **Gradle**: Upgraded from 7.5 to 8.5 (supports Java 21)
- **Android Gradle Plugin**: Updated from 7.4.2 to 8.1.4
- **Java Target**: Updated compile/kotlin targets from 1.8 to 17 for consistency
- **Environment**: Explicitly using Java 17 for build stability

### Dependency Updates
**Decision**: Updated build.gradle files with:
- **Hilt**: For dependency injection and proper scoping of ViewModels/Repositories
- **DataStore**: Modern replacement for SharedPreferences for timer state persistence
- **Testing Libraries**: Added kotlinx-coroutines-test, Turbine, MockK for comprehensive Flow testing
- **Namespace Migration**: Moved package declaration from AndroidManifest to build.gradle (modern practice)

**Why?**: These dependencies establish the foundation for:
1. Testable architecture with proper dependency injection
2. Flow-based reactive programming with robust testing support
3. State persistence across app restarts using DataStore

### Hilt Integration
**Implementation**: Added Hilt dependency injection setup:
- Created `PictureInPictureApplication` with `@HiltAndroidApp`
- Added `@AndroidEntryPoint` to both MainActivity and MovieActivity
- Updated AndroidManifest to register the Application class

**Result**: Build successful with Hilt integration. Ready for ViewModel dependency injection.

## Phase 1.2: LiveData to Flow Migration (Completed)

### ViewModel Modernization
**Problem**: The original `MainViewModel` used LiveData, which while functional, lacks the power and composability of Flow.

**Implementation**:
- Replaced `MutableLiveData<Boolean>` with `MutableStateFlow<Boolean>` for started state
- Replaced `MutableLiveData<Long>` with `MutableStateFlow<Long>` for time tracking
- Converted time formatting from `LiveData.map()` to `Flow.map().stateIn()` with `WhileSubscribed(5000)`
- Added `@HiltViewModel` annotation and `@Inject` constructor for dependency injection

### Activity Integration
**Updated MainActivity**:
- Replaced `LiveData.observe()` with `Flow.collect()` inside `repeatOnLifecycle`
- Used separate coroutine launches for each state collection
- Maintained proper lifecycle awareness to prevent memory leaks

**Benefits Achieved**:
1. **Better Performance**: `SharingStarted.WhileSubscribed(5000)` stops upstream when no active observers
2. **Compose Readiness**: StateFlow integrates seamlessly with Compose `collectAsState()`
3. **Testing Simplicity**: Flow testing is more straightforward than LiveData testing
4. **Modern Patterns**: Follows current Android architecture recommendations

**Testing**: Build successful with APK generation. Ready for comprehensive unit testing.



---

*Will continue updating this diary as implementation progresses, documenting decisions, challenges, and learnings along the way.*