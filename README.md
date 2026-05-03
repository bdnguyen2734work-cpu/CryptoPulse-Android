# 📈 CryptoPulse Android

Ứng dụng Android theo dõi thị trường crypto thời gian thực.

## ✨ Tính năng
- Giá coin realtime qua Binance WebSocket
- Biểu đồ candlestick & phân tích kỹ thuật (RSI, MACD, Bollinger Bands)
- Fear & Greed Index tự tính
- Whale News dịch tiếng Việt
- On-chain wallet tracker
- Đăng nhập Email / Google (Firebase)
- Upload avatar (Cloudinary)
- Admin dashboard

## 🛠️ Tech Stack
- Java + Android SDK (minSdk 26)
- Firebase Auth + FCM
- Retrofit2 + OkHttp
- Binance WebSocket
- MPAndroidChart
- Cloudinary (avatar)
- Data Binding + ViewModel
## 📸 Screenshots

### 🏠 Home & Market
<p float="left">
  <img src="screenshots/home.png" width="180"/>
  <img src="screenshots/home1.png" width="180"/>
  <img src="screenshots/market.png" width="180"/>
  <img src="screenshots/coinlist.png" width="180"/>
  <img src="screenshots/coinlist1.png" width="180"/>
</p>

### 📊 Analysis
<p float="left">
  <img src="screenshots/analysis.png" width="180"/>
  <img src="screenshots/analysis1.png" width="180"/>
  <img src="screenshots/analysis2.png" width="180"/>
</p>

### 📰 News
<p float="left">
  <img src="screenshots/news.png" width="180"/>
  <img src="screenshots/news1.png" width="180"/>
  <img src="screenshots/news2.png" width="180"/>
</p>

### 💼 Wallet
<p float="left">
  <img src="screenshots/wallet.png" width="180"/>
  <img src="screenshots/wallet1.png" width="180"/>
  <img src="screenshots/wallet2.png" width="180"/>
</p>

### 🔐 Auth
<p float="left">
  <img src="screenshots/login.png" width="180"/>
  <img src="screenshots/register.png" width="180"/>
  <img src="screenshots/search.png" width="180"/>
</p>

## 🚀 Setup
1. Clone repo
2. Thêm `google-services.json` vào `/app/`
3. Cập nhật backend URL trong `AppPrefs.java`
4. Run trên Android Studio

## 🌐 Backend
[CryptoPulse-Backend](https://github.com/bdnguyen2734work-cpu/CryptoPulse-Backend)