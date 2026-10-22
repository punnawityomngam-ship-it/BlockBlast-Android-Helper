# Block Blast Helper (Android)

ตัวช่วยเล่นเกม **Block Blast!** บน Android  
จับภาพหน้าจอ → วิเคราะห์ → แสดงคำแนะนำตำแหน่งวางบล็อกแบบ Floating Window

## คุณสมบัติหลัก

- ✅ ขออนุญาต Screen Capture (MediaProjection)
- ✅ Foreground Service จับภาพต่อเนื่อง
- ✅ Floating Overlay แสดงคำแนะนำทับเกมได้ (ลากย้ายได้)
- ✅ Solver ที่เน้น **รอดนาน + คอมโบ + รักษาพื้นที่ 3×3**
- ✅ โครงสร้างพร้อมสำหรับต่อ Computer Vision จริง

## โครงสร้างโปรเจกต์

```
app/src/main/java/com/blockblast/helper/
├── MainActivity.kt              # UI หลัก
├── data/Models.kt               # Board, Shape, Placement
├── solver/BlockSolver.kt        # อัลกอริทึมหาตำแหน่งดีที่สุด
├── vision/BoardDetector.kt      # ตรวจจับกระดานจากภาพ (placeholder)
└── service/
    ├── ScreenCaptureService.kt  # จับภาพหน้าจอ
    └── OverlayService.kt        # หน้าต่างลอย
```

## วิธีติดตั้งและรัน

1. เปิดโปรเจกต์ด้วย **Android Studio** (Hedgehog หรือใหม่กว่า)
2. Sync Gradle
3. เชื่อมต่อมือถือ Android (API 26+) หรือใช้ Emulator
4. กด Run

### สิทธิ์ที่ต้องอนุญาต
- **Screen Capture** → กดอนุญาตเมื่อระบบถาม
- **Display over other apps** (Floating Window) → ไปที่ Settings ของแอป

## วิธีใช้

1. เปิดแอป → กด "เปิดสิทธิ์ Floating Window"
2. กด "เริ่มจับภาพหน้าจอ" → อนุญาต
3. สลับไปเปิดเกม Block Blast!
4. หน้าต่างลอยจะแสดงคำแนะนำ
5. ลากย้ายหน้าต่างได้ตามสะดวก

## พัฒนาต่อ (สำคัญ)

ไฟล์ `BoardDetector.kt` ตอนนี้ยังเป็น **mock**  
คุณต้องเพิ่ม logic จริง เช่น:

- ใช้ OpenCV หาตำแหน่งกริด 8×8
- หรือเทรนโมเดล TFLite / ML Kit ให้จำ UI ของเกม
- ตรวจจับสีบล็อกและรูปร่างชิ้นด้านล่าง

Solver (`BlockSolver.kt`) พร้อมใช้งานแล้ว และสามารถปรับน้ำหนักคะแนนได้ตามต้องการ

## ข้อจำกัด

- การจับภาพหน้าจอต้องให้ผู้ใช้กดอนุญาตทุกครั้งที่เริ่ม
- บนบางเครื่องผู้ผลิต (Xiaomi, Oppo ฯลฯ) อาจต้องเปิดสิทธิ์เพิ่มใน Settings
- Computer Vision ยังต้องพัฒนาต่อเพื่อความแม่นยำสูง

## License

MIT — ใช้พัฒนาต่อได้自由
