# StarWorks Firebase Workspace

This folder contains the Firebase-first migration scaffold for StarWorks.

## Layout
- `firebase.json`: Hosting, Functions, Firestore, Storage, and Data Connect configuration
- `functions/`: TypeScript Cloud Functions entrypoint
- `firestore.rules`, `firestore.indexes.json`: Firestore security and indexes
- `storage.rules`: Firebase Storage security rules
- `dataconnect/`: relational schema reference for structured business modules

## Start
1. Copy `.firebaserc.example` to `.firebaserc`
2. Fill in the real Firebase project id
3. Install and build Functions

```powershell
cd functions
npm install
npm run build
```

4. Build the frontend bundle

```powershell
cd ..\..\frontend
npm install
npm run build
```

5. Deploy Firebase resources

```powershell
cd ..\firebase
npm run deploy
```

## Current scope
- Firebase Auth session bootstrap path on the frontend
- Firestore realtime messenger foundation
- Firebase Storage upload helper path
- Data Connect schema seed for relational modules

The current Spring Boot backend is still the feature reference. This workspace is intended to let you replace domains incrementally while keeping the current app available.
