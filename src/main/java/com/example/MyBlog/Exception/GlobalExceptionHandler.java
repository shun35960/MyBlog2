package com.example.MyBlog.Exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * GlobalExceptionHandlerは、アプリケーション全体の例外処理を一元管理するクラスです。
 * バリデーションエラー、404エラー、一般的な例外を処理します。
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * バリデーションエラーを処理します
     * @param ex MethodArgumentNotValidException
     * @param model モデル
     * @return エラーページ
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleValidationExceptions(MethodArgumentNotValidException ex, Model model) {
        StringBuilder errorMessages = new StringBuilder();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String errorMessage = error.getDefaultMessage();
            errorMessages.append(errorMessage).append("\n");
        });

        log.warn("Validation error: {}", errorMessages);
        model.addAttribute("errorTitle", "入力エラー");
        model.addAttribute("errorMessage", errorMessages.toString());
        return "error";
    }

    /**
     * IllegalArgumentExceptionを処理します（記事が見つからない場合など）
     * @param ex IllegalArgumentException
     * @param model モデル
     * @return エラーページ
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleIllegalArgumentException(IllegalArgumentException ex, Model model) {
        log.error("IllegalArgumentException occurred: {}", ex.getMessage());
        model.addAttribute("errorTitle", "リソースが見つかりません");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    /**
     * 404エラーを処理します
     * @param ex NoHandlerFoundException
     * @param model モデル
     * @return エラーページ
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFoundException(NoHandlerFoundException ex, Model model) {
        log.warn("404 Not Found: {}", ex.getRequestURL());
        model.addAttribute("errorTitle", "ページが見つかりません");
        model.addAttribute("errorMessage", "お探しのページは存在しないか、移動した可能性があります。");
        return "error";
    }

    /**
     * その他の予期しない例外を処理します
     * @param ex Exception
     * @param model モデル
     * @return エラーページ
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGlobalException(Exception ex, Model model) {
        log.error("Unexpected error occurred", ex);
        model.addAttribute("errorTitle", "エラーが発生しました");
        model.addAttribute("errorMessage", "予期しないエラーが発生しました。管理者にお問い合わせください。");
        return "error";
    }
}