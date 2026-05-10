package dz.freelance.modules.auth.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dz.freelance.modules.auth.dto.AuthDtos.*;
import dz.freelance.modules.auth.dto.AuthDtos.AuthResponse;
import dz.freelance.modules.auth.dto.AuthDtos.ForgotPasswordRequest;
import dz.freelance.modules.auth.dto.AuthDtos.LoginRequest;
import dz.freelance.modules.auth.dto.AuthDtos.RefreshTokenRequest;
import dz.freelance.modules.auth.dto.AuthDtos.RegisterRequest;
import dz.freelance.modules.auth.dto.AuthDtos.ResendOtpRequest;
import dz.freelance.modules.auth.dto.AuthDtos.ResetPasswordRequest;
import dz.freelance.modules.auth.dto.AuthDtos.UserSummary;
import dz.freelance.modules.auth.dto.AuthDtos.VerifyEmailRequest;
import dz.freelance.modules.user.entity.User;
import dz.freelance.modules.user.entity.User.ProviderType;
import dz.freelance.modules.user.entity.User.UserRole;
import dz.freelance.modules.user.repository.UserRepository;
import dz.freelance.shared.exception.AppException;
import dz.freelance.shared.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String mailFrom;

    @Value("${app.otp.expiration-minutes:10}")
    private int otpExpirationMinutes;

    // In-memory stores (replace Redis for local dev)
    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();
    private final Map<String, String> refreshTokenStore = new ConcurrentHashMap<>();

    private record OtpEntry(String otp, LocalDateTime expiry) {}

    // ── Register ──────────────────────────────────────────

    @Transactional
    public String register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail().toLowerCase())) {
            throw new AppException("Un compte existe déjà avec cet email", HttpStatus.CONFLICT);
        }

        validateProviderFields(req);

        User user = User.builder()
            .email(req.getEmail().toLowerCase())
            .password(passwordEncoder.encode(req.getPassword()))
            .role(req.getRole())
            .firstName(req.getFirstName())
            .lastName(req.getLastName())
            .phoneNumber(req.getPhoneNumber())
            .city(req.getCity())
            .wilaya(req.getWilaya())
            .providerType(req.getRole() == UserRole.PROVIDER ? req.getProviderType() : null)
            .businessName(req.getBusinessName())
            .businessDescription(req.getBusinessDescription())
            .websiteUrl(req.getWebsiteUrl())
            .build();

        userRepository.save(user);

        // TODO: re-enable when mail is configured
        // sendOtpEmail(user.getEmail(), "verify");

        log.info("Nouvel utilisateur enregistré: {} ({})", user.getEmail(), user.getRole());
        return "Inscription réussie. Vous pouvez maintenant vous connecter.";
    }

    // ── Login ─────────────────────────────────────────────

    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.getEmail().toLowerCase(), req.getPassword())
        );

        User user = userRepository.findByEmail(req.getEmail().toLowerCase())
            .orElseThrow(() -> new AppException("Utilisateur introuvable", HttpStatus.NOT_FOUND));

        // TODO: re-enable when email verification flow is ready
        // if (!user.isEmailVerified()) {
        //     throw new AppException("Veuillez vérifier votre email avant de vous connecter", HttpStatus.FORBIDDEN);
        // }

        if (!user.isActive()) {
            throw new AppException("Votre compte a été suspendu. Contactez le support.", HttpStatus.FORBIDDEN);
        }

        var userDetails = org.springframework.security.core.userdetails.User
            .withUsername(user.getEmail())
            .password(user.getPassword())
            .roles(user.getRole().name())
            .build();

        String accessToken = jwtUtil.generateAccessToken(userDetails, user.getId(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        refreshTokenStore.put(String.valueOf(user.getId()), refreshToken);

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .user(toSummary(user))
            .build();
    }

    // ── Verify email (OTP) ────────────────────────────────

    @Transactional
    public String verifyEmail(VerifyEmailRequest req) {
        String key = "otp:verify:" + req.getEmail().toLowerCase();
        OtpEntry entry = otpStore.get(key);

        if (entry == null || !entry.otp().equals(req.getOtp()) || entry.expiry().isBefore(LocalDateTime.now())) {
            throw new AppException("Code OTP invalide ou expiré", HttpStatus.BAD_REQUEST);
        }

        User user = userRepository.findByEmail(req.getEmail().toLowerCase())
            .orElseThrow(() -> new AppException("Utilisateur introuvable", HttpStatus.NOT_FOUND));

        user.setEmailVerified(true);
        userRepository.save(user);
        otpStore.remove(key);

        log.info("Email vérifié: {}", user.getEmail());
        return "Email vérifié avec succès. Vous pouvez maintenant vous connecter.";
    }

    // ── Resend OTP ────────────────────────────────────────

    public String resendOtp(ResendOtpRequest req) {
        if (!userRepository.existsByEmail(req.getEmail().toLowerCase())) {
            throw new AppException("Aucun compte trouvé avec cet email", HttpStatus.NOT_FOUND);
        }
        sendOtpEmail(req.getEmail().toLowerCase(), "verify");
        return "Code OTP renvoyé par email.";
    }

    // ── Refresh token ─────────────────────────────────────

    public AuthResponse refreshToken(RefreshTokenRequest req) {
        String email = jwtUtil.extractUsername(req.getRefreshToken());
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new AppException("Token invalide", HttpStatus.UNAUTHORIZED));

        String stored = refreshTokenStore.get(String.valueOf(user.getId()));
        if (stored == null || !stored.equals(req.getRefreshToken())) {
            throw new AppException("Refresh token invalide ou expiré", HttpStatus.UNAUTHORIZED);
        }

        var userDetails = org.springframework.security.core.userdetails.User
            .withUsername(user.getEmail())
            .password(user.getPassword())
            .roles(user.getRole().name())
            .build();

        String newAccess = jwtUtil.generateAccessToken(userDetails, user.getId(), user.getRole().name());
        String newRefresh = jwtUtil.generateRefreshToken(userDetails);

        refreshTokenStore.put(String.valueOf(user.getId()), newRefresh);

        return AuthResponse.builder()
            .accessToken(newAccess)
            .refreshToken(newRefresh)
            .user(toSummary(user))
            .build();
    }

    // ── Forgot / Reset password ───────────────────────────

    public String forgotPassword(ForgotPasswordRequest req) {
        if (userRepository.existsByEmail(req.getEmail().toLowerCase())) {
            sendOtpEmail(req.getEmail().toLowerCase(), "reset");
        }
        return "Si un compte existe, un email de réinitialisation a été envoyé.";
    }

    @Transactional
    public String resetPassword(ResetPasswordRequest req) {
        String key = "otp:reset:" + req.getEmail().toLowerCase();
        OtpEntry entry = otpStore.get(key);

        if (entry == null || !entry.otp().equals(req.getOtp()) || entry.expiry().isBefore(LocalDateTime.now())) {
            throw new AppException("Code OTP invalide ou expiré", HttpStatus.BAD_REQUEST);
        }

        User user = userRepository.findByEmail(req.getEmail().toLowerCase())
            .orElseThrow(() -> new AppException("Utilisateur introuvable", HttpStatus.NOT_FOUND));

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
        otpStore.remove(key);
        refreshTokenStore.remove(String.valueOf(user.getId()));

        return "Mot de passe réinitialisé avec succès.";
    }

    // ── Logout ────────────────────────────────────────────

    public void logout(String userId) {
        refreshTokenStore.remove(userId);
    }

    // ── Helpers ───────────────────────────────────────────

    private void sendOtpEmail(String email, String type) {
        String otp = generateOtp();
        String key = "otp:" + type + ":" + email;
        otpStore.put(key, new OtpEntry(otp, LocalDateTime.now().plusMinutes(otpExpirationMinutes)));

        String subject = type.equals("verify")
            ? "Vérification de votre email — Freelance DZ"
            : "Réinitialisation de mot de passe — Freelance DZ";
        String body = type.equals("verify")
            ? "Bonjour,\n\nVotre code de vérification est : " + otp + "\n\nCe code expire dans " + otpExpirationMinutes + " minutes."
            : "Bonjour,\n\nVotre code de réinitialisation est : " + otp + "\n\nCe code expire dans " + otpExpirationMinutes + " minutes.";

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(email);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Erreur envoi email OTP à {}: {}", email, e.getMessage());
        }
    }

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        return String.format("%06d", random.nextInt(999999));
    }

    private void validateProviderFields(RegisterRequest req) {
        if (req.getRole() == UserRole.PROVIDER) {
            if (req.getProviderType() == null) {
                throw new AppException("Le type de prestataire est obligatoire (PERSON ou ORGANISM)", HttpStatus.BAD_REQUEST);
            }
            if (req.getProviderType() == ProviderType.ORGANISM && (req.getBusinessName() == null || req.getBusinessName().isBlank())) {
                throw new AppException("Le nom de l'organisme est obligatoire", HttpStatus.BAD_REQUEST);
            }
        }
    }

    private UserSummary toSummary(User user) {
        return UserSummary.builder()
            .id(user.getId())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .avatarUrl(user.getAvatarUrl())
            .role(user.getRole())
            .providerType(user.getProviderType())
            .emailVerified(user.isEmailVerified())
            .verified(user.isVerified())
            .build();
    }
}