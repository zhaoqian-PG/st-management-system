package com.stmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDTO {

    private Long id;

    @Size(max = 20, message = "顧客番号は20文字以内で入力してください")
    private String customerCode;

    @NotBlank(message = "会社名は必須です")
    @Size(max = 200, message = "会社名は200文字以内で入力してください")
    private String companyName;

    @Size(max = 100, message = "社長名は100文字以内で入力してください")
    private String presidentName;

    @Size(max = 500, message = "Webサイトは500文字以内で入力してください")
    private String website;

    @Size(max = 500, message = "住所は500文字以内で入力してください")
    private String address;

    @Size(max = 100, message = "担当者名は100文字以内で入力してください")
    private String contactName;

    @Size(max = 255, message = "メールアドレスは255文字以内で入力してください")
    @Pattern(regexp = "^$|^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$", message = "メールアドレスの形式が正しくありません")
    private String email;

    @Size(max = 20, message = "電話番号は20文字以内で入力してください")
    private String phone;

    @Size(max = 100, message = "営業担当者名は100文字以内で入力してください")
    private String salesRepName;

    @Size(max = 20, message = "営業担当連絡先は20文字以内で入力してください")
    private String salesRepPhone;

    @Size(max = 255, message = "営業担当メールは255文字以内で入力してください")
    @Pattern(regexp = "^$|^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$", message = "営業担当メールの形式が正しくありません")
    private String salesRepEmail;

    @Size(max = 100, message = "事務担当者名は100文字以内で入力してください")
    private String adminRepName;

    @Size(max = 20, message = "事務担当連絡先は20文字以内で入力してください")
    private String adminRepPhone;

    @Size(max = 255, message = "事務担当メールは255文字以内で入力してください")
    @Pattern(regexp = "^$|^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$", message = "事務担当メールの形式が正しくありません")
    private String adminRepEmail;

    @Size(max = 500, message = "取引番号は500文字以内で入力してください")
    private String torihikiNo;

    // 紐付く銀行口座一覧（詳細取得時のみ）
    private List<BankAccountDTO> bankAccounts;
}
