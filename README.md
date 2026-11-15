# Hệ thống quản lý phòng khám

Ứng dụng desktop quản lý quy trình khám tại phòng khám: tiếp nhận bệnh nhân, khám bệnh, xét nghiệm, kê đơn và cấp phát thuốc. Mỗi nhân viên đăng nhập bằng một vai trò và chỉ thấy các màn hình của vai trò đó.

## Cách các phần nói chuyện với nhau

Hệ thống gồm ba module Maven và hai chương trình chạy riêng.

| Module | Vai trò |
|---|---|
| `client` | Giao diện JavaFX |
| `server` | Spring Boot, xử lý nghiệp vụ và lưu MariaDB |
| `shared` | Model, DTO và interface RMI dùng chung cho hai phía |

Client không truy cập database. Mọi thao tác ghi và đọc đi qua RMI:

`client` --RMI `rmi://localhost:1099/ClinicService`--> `server` --> MariaDB `clinic_system`

Khi dữ liệu đổi, server phát một sự kiện WebSocket tới `ws://localhost:8080/ws`, topic `/topic/realtime`. Client đang mở chỉ tải lại màn hình liên quan, ví dụ đổi bệnh nhân thì màn lễ tân và bác sĩ được làm mới, màn cấp phát thuốc thì không.

Server phải chạy trước. Client kết nối lúc mở; nếu không thấy RMI thì màn hình báo lỗi thay vì màn đăng nhập.

## Luồng khám

1. Lễ tân tiếp nhận bệnh nhân mới hoặc bệnh nhân cũ. Có bác sĩ thì trạng thái là **Đã phân công**, chưa có thì là **Chờ xử lý**.
2. Bác sĩ nhận bệnh nhân vào khám (**Đang khám**), viết hồ sơ bệnh án. Nếu cần xét nghiệm, bệnh nhân chuyển sang **Chờ xét nghiệm**. Nếu không, trạng thái là **Đã khám**.
3. Nhân viên xét nghiệm nhập kết quả. Bệnh nhân chuyển sang **Đã có kết quả xét nghiệm**.
4. Bác sĩ kê đơn khi bệnh nhân đã khám hoặc đã có kết quả. Toa ở trạng thái **Mới tạo**, bệnh nhân là **Đã kê đơn**. Mã toa có dạng `DT001`, `DT002`, ...
5. Dược sĩ cấp phát toa **Mới tạo**. Toa thành **Đã cấp phát**, bệnh nhân thành **Đã hoàn tất**.

Quản trị xem và sửa người dùng, bệnh nhân, toa thuốc, hồ sơ bệnh án. Mọi vai trò đều có màn hồ sơ cá nhân để đổi mật khẩu.

Xóa bệnh nhân chỉ được khi không còn hồ sơ, toa thuốc, kết quả xét nghiệm, và lần khám gần nhất đã quá 3 năm. Xóa người dùng bị chặn nếu người đó còn gắn với bệnh nhân hoặc dữ liệu khám.

## Trợ lý AI

Trợ lý chỉ đọc và điền form. Người dùng vẫn tự bấm **Lưu hồ sơ** hoặc **Tiếp nhận**. AI không đổi trạng thái bệnh nhân, không xóa dữ liệu, không kê đơn và không cấp phát thuốc.

| Màn hình | Nút | Việc trợ lý làm |
|---|---|---|
| Bác sĩ | **Tóm tắt** | Viết một đoạn tóm tắt hồ sơ cũ của bệnh nhân đang chọn. Chỉ bác sĩ được phân công bệnh nhân đó mới xem được. |
| Bác sĩ | **Gợi ý từ ghi chú** | Đọc ghi chú khám, điền chẩn đoán, ghi chú đã viết lại và ô cần xét nghiệm. |
| Lễ tân | **Điền từ mô tả** | Tách một câu mô tả thành họ tên, tuổi, giới tính, số điện thoại và địa chỉ. Ô trống hoặc tuổi bằng 0 được giữ nguyên. Mã bệnh nhân và bác sĩ không bị đổi. |

Client không giữ API key. Nút bấm gọi RMI tới server, server mới gọi API tương thích OpenAI (`/chat/completions`). Gọi AI chạy ở luồng nền để cửa sổ JavaFX không bị đơ. Khi tắt trợ lý, thiếu địa chỉ, thiếu key hoặc thiếu tên model, nút báo **Chưa bật trợ lý AI** và không gọi mạng. JSON lỗi, hết thời gian chờ hoặc API lỗi thì form giữ nguyên và hiện thông báo tiếng Việt.

Trong `server/src/main/resources/application.properties`:

```properties
clinic.ai.enabled=false
clinic.ai.base-url=
clinic.ai.api-key=
clinic.ai.model=
```

Để dùng Gemini ở gói miễn phí, lấy key tại [Google AI Studio](https://aistudio.google.com/apikey) rồi điền:

```properties
clinic.ai.enabled=true
clinic.ai.base-url=https://generativelanguage.googleapis.com/v1beta/openai
clinic.ai.api-key=your_gemini_key
clinic.ai.model=gemini-2.5-flash
```

Sửa xong phải khởi động lại `ClinicServerApplication`. Không commit `clinic.ai.api-key` và mật khẩu database lên git. Chỉ thử trợ lý với bệnh nhân mẫu, vì gói miễn phí có thể dùng nội dung gửi lên để cải thiện mô hình.

## Chạy dự án

Cần JDK 17 trở lên, Maven và MariaDB đang chạy ở `localhost:3306`.

Sửa tài khoản database trong `server/src/main/resources/application.properties` cho đúng máy của bạn:

```properties
spring.datasource.username=root
spring.datasource.password=your_password
```

Lần đầu server chạy, Hibernate tạo database `clinic_system` và các bảng. Nếu bảng người dùng còn trống, server tạo dữ liệu mẫu.

Trong IntelliJ, chạy lần lượt hai lớp `main`:

1. `com.clinic.server.ClinicServerApplication`
2. `com.clinic.client.MainApp`

Hoặc bằng Maven, mỗi lệnh một terminal:

```bash
mvn -pl server spring-boot:run
mvn -pl client javafx:run
```

## Tài khoản mẫu

Mật khẩu của tất cả tài khoản dưới đây là `123`. Chúng chỉ được tạo khi bảng `users` chưa có dòng nào.

| Tài khoản | Vai trò |
|---|---|
| `quantri` | Quản trị |
| `letan` | Lễ tân |
| `bacsi` | Bác sĩ |
| `duocsi` | Dược sĩ |
| `xetnghiem` | Xét nghiệm |

Dữ liệu mẫu còn có bốn bệnh nhân `BN001`–`BN004` và vài toa thuốc gắn với bác sĩ `bacsi`.
