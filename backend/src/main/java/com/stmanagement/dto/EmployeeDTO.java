package com.stmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDTO {

    private Long id;

    private String employeeCode;

    @NotBlank(message = "氏名は必須です")
    @Size(max = 100, message = "氏名は100文字以内で入力してください")
    private String name;

    @Size(max = 255, message = "メールアドレスは255文字以内で入力してください")
    @Pattern(regexp = "^$|^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$", message = "正しいメールアドレス形式で入力してください")
    private String email;

    @Size(max = 20, message = "電話番号は20文字以内で入力してください")
    private String phone;

    @Size(max = 500, message = "日本住所は500文字以内で入力してください")
    private String japanAddress;

    @Size(max = 500, message = "中国住所は500文字以内で入力してください")
    private String chinaAddress;

    @Size(max = 20, message = "中国電話番号は20文字以内で入力してください")
    private String chinaPhone;

    @Size(max = 100, message = "中国緊急連絡先は100文字以内で入力してください")
    private String chinaEmergencyContact;

    @Size(max = 100, message = "部署は100文字以内で入力してください")
    private String department;

    @Size(max = 100, message = "役職は100文字以内で入力してください")
    private String position;

    private String torihikiNo;
    private LocalDate joinDate;
    private LocalDate birthDate;
    private String attachmentPath;

    private List<BankAccountDTO> bankAccounts;
}
