package com.odontoflow.controller;

import com.odontoflow.dto.request.UpdateMeRequest;
import com.odontoflow.dto.request.UpdatePasswordRequest;
import com.odontoflow.dto.response.AuthResponse;
import com.odontoflow.dto.response.MeResponse;
import com.odontoflow.dto.response.MessageResponse;
import com.odontoflow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
@Tag(name = "Meu Perfil", description = "Dados, senha e foto do usuário autenticado")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Meu perfil", description = "Retorna os dados do usuário autenticado")
    @ApiResponse(responseCode = "200", description = "Dados retornados")
    public ResponseEntity<MeResponse> getMe() {
        return ResponseEntity.ok(userService.getMe());
    }

    @PutMapping
    @Operation(summary = "Atualizar perfil", description = "Edita nome e e-mail. Reemite o token JWT com os novos dados.")
    @ApiResponse(responseCode = "200", description = "Perfil atualizado")
    @ApiResponse(responseCode = "400", description = "E-mail já cadastrado por outro usuário")
    @ApiResponse(responseCode = "422", description = "Erro de validação nos campos")
    public ResponseEntity<AuthResponse> updateMe(@Valid @RequestBody UpdateMeRequest request) {
        return ResponseEntity.ok(userService.updateMe(request));
    }

    @PutMapping("/password")
    @Operation(summary = "Alterar senha", description = "Valida a senha atual antes de gravar a nova")
    @ApiResponse(responseCode = "200", description = "Senha alterada")
    @ApiResponse(responseCode = "400", description = "Senha atual incorreta")
    @ApiResponse(responseCode = "422", description = "Erro de validação nos campos")
    public ResponseEntity<MessageResponse> updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        return ResponseEntity.ok(userService.updatePassword(request));
    }

    @PostMapping(value = "/avatar", consumes = "multipart/form-data")
    @Operation(summary = "Upload da foto de perfil", description = "multipart/form-data com campo 'file'. Aceitos: PNG, JPG, JPEG, WEBP. Máx. 2 MB. Foto anterior é removida do storage.")
    @ApiResponse(responseCode = "200", description = "Foto enviada")
    @ApiResponse(responseCode = "400", description = "Arquivo inválido")
    public ResponseEntity<MeResponse> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(userService.uploadAvatar(file));
    }

    @DeleteMapping("/avatar")
    @Operation(summary = "Remover foto de perfil", description = "Remove a foto e volta a exibir as iniciais")
    @ApiResponse(responseCode = "200", description = "Foto removida")
    public ResponseEntity<MeResponse> deleteAvatar() {
        return ResponseEntity.ok(userService.deleteAvatar());
    }
}
