package com.stmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankAccountDTO {

    private Long id;
    private String torihikiNo;
    private String branchNo;

    private String category;
    private Long customerId;
    private Long employeeId;
    private String customerName;
    private String employeeName;

    @Size(max = 10, message = "支店番号は最大10文字です")
    private String branchCode;

    @NotBlank(message = "銀行名称は必須です")
    @Size(max = 200, message = "銀行名称は最大200文字です")
    private String bankName;

    @NotBlank(message = "口座種類は必須です")
    @Pattern(regexp = "普通|当座", message = "口座種類は「普通」または「当座」で入力してください")
    private String accountType;

    @NotBlank(message = "口座番号は必須です")
    @Size(max = 50, message = "口座番号は最大50文字です")
    private String accountNumber;

    @NotBlank(message = "口座名義は必須です")
    @Size(max = 100, message = "口座名義は最大100文字です")
    private String accountHolder;

    private Boolean isDefault;
}
