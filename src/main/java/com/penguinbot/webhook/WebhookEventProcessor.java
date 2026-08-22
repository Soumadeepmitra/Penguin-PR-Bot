package com.penguinbot.webhook;

import com.penguinbot.command.CommandContext;
import com.penguinbot.command.CommandHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.penguinbot.config.RepoConfig;
import com.penguinbot.config.RepoConfigService;
import com.penguinbot.github.GitHubApiService;
import org.kohsuke.github.GHRepository;

import java.util.List;

@Service
public class WebhookEventProcessor {
    private static final Logger log = LoggerFactory.getLogger(WebhookEventProcessor.class);
    private final List<CommandHandler> commandHandlers;
    private final GitHubApiService gitHubApiService;
    private final RepoConfigService repoConfigService;

    public WebhookEventProcessor(List<CommandHandler> commandHandlers,
                                 GitHubApiService gitHubApiService,
                                 RepoConfigService repoConfigService) {
        this.commandHandlers = commandHandlers;
        this.gitHubApiService = gitHubApiService;
        this.repoConfigService = repoConfigService;
    }

    @Async("webhookExecutor")
    public void processCommand(CommandContext context) {
        commandHandlers.stream()
            .filter(h -> h.commandName().equals(context.commandName()))
            .findFirst()
            .ifPresent(handler -> {
                try {
                    handler.handle(context);
                } catch (Exception e) {
                    log.error("Error processing command: {}", context.commandName(), e);
                }
            });
    }

    @Async("webhookExecutor")
    public void processPrSynchronized(long installationId, String owner, String repo, int prNumber, String senderLogin) {
        try {
            log.info("Processing PR #{} synchronize (commits pushed / fix applied)", prNumber);

            // 1. Mark previous review comments as outdated
            String botLogin = gitHubApiService.getAuthenticatedBotLogin(installationId);
            gitHubApiService.markPreviousReviewsAsOutdated(installationId, owner, repo, prNumber, botLogin);

            // 2. Automatically re-review if autoReview is enabled in repo config
            GHRepository ghRepo = gitHubApiService.getRepository(installationId, owner, repo);
            RepoConfig config = repoConfigService.loadConfig(ghRepo);

            if (config.isAutoReview()) {
                CommandContext reviewContext = new CommandContext(
                        "review",
                        "",
                        installationId,
                        owner,
                        repo,
                        prNumber,
                        0L,
                        senderLogin
                );
                processCommand(reviewContext);
            }
        } catch (Exception e) {
            log.error("Error processing PR synchronize for PR #{}", prNumber, e);
        }
    }
}
