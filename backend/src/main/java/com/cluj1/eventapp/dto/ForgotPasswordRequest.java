package com.cluj1.eventapp.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordRequest {
    @NotBlank(message = "Email is required.")
    @Email(message = "Invalid email format.")
    @Pattern(
        regexp = "^[a-zA-Z0-9]+\\.[a-zA-Z0-9]+@msg\\.group$",
        message = "Invalid email format"
    )
    private String email;
}
