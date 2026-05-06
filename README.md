📱 LuluFindy Application Setup & User Guide
🧩 Step 1: Installation & Initial Setup
🔽 Install Android Studio

Download and install Android Studio (Electric Eel | 2022.1.1 – January 12, 2023).

📥 Clone the Project
Go to the GitHub repository: https://github.com/kenanidou/AndroidProject)
Click the green “Code” button and copy the repository URL.
Open Android Studio and select “Get from VCS”.
Paste the URL and clone the project.
🔑 Configure API Key

To ensure the maps function properly, add your API key in two places:

AndroidManifest.xml (line 23)
AppConfig class (line 4)

Replace the placeholder API string with the provided key.

📍 Set Device Location

The application is configured to run in Honolulu.
Set your emulator or physical device location accordingly for proper functionality.

▶️ Run the Application

Build and run the project normally through Android Studio.

🔐 Step 2: Login, Registration & Password Recovery
1️⃣ Login or Register
Enter your credentials and click “Login” if you already have an account.
Or register to create a new account.
After successful login:
Regular users are redirected to the User Menu
Admin users are redirected to the Admin Menu
2️⃣ Invalid Credentials

If incorrect login details are entered, access will be denied.

3️⃣ Password Recovery
Click “Forgot Password?”
Enter your email address
You will receive a temporary password
Enter:
Your new password
The temporary password provided
If successful, you will be redirected to the menu
4️⃣ User Menu Features

The following options are available:

🚗 Start Parking
🔍 Search Location
💳 Wallet
📊 User Activity
👤 Profile
📅 Calendar
5️⃣ Profile & Calendar
Profile: Update your personal information stored in the database
Calendar:
🟢 Green → Regular working days
🔵 Blue → Special schedule days
🔴 Red → Holidays
