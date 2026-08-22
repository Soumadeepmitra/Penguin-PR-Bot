package com.penguinbot.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.penguinbot.command.CommandContext;
import com.penguinbot.command.CommandParser;
import com.penguinbot.config.RepoConfig;
import com.penguinbot.config.RepoConfigService;
import com.penguinbot.github.GitHubApiService;
import com.penguinbot.github.dto.IssueCommentPayload;
import com.penguinbot.github.dto.PullRequestPayload;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/webhook")
public class WebhookController {
    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final WebhookSignatureVerifier signatureVerifier;
    private final CommandParser commandParser;
    private final ObjectMapper objectMapper;
    private final GitHubApiService gitHubApiService;
    private final RepoConfigService repoConfigService;
    private final WebhookEventProcessor eventProcessor;

    public WebhookController(WebhookSignatureVerifier signatureVerifier,
                             CommandParser commandParser,
                             ObjectMapper objectMapper,
                             GitHubApiService gitHubApiService,
                             RepoConfigService repoConfigService,
                             WebhookEventProcessor eventProcessor) {
        this.signatureVerifier = signatureVerifier;
        this.commandParser = commandParser;
        this.objectMapper = objectMapper;
        this.gitHubApiService = gitHubApiService;
        this.repoConfigService = repoConfigService;
        this.eventProcessor = eventProcessor;
    }

    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestHeader("X-GitHub-Event") String event,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody String payload) {

        if (!signatureVerifier.isValid(signature, payload)) {
            log.warn("Invalid webhook signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
        }

        log.info("Received webhook event: {}", event);

        try {
            switch (event) {
                case "issue_comment" -> handleIssueComment(payload);
                case "pull_request" -> handlePullRequest(payload);
                default -> log.debug("Ignoring event: {}", event);
            }
        } catch (Exception e) {
            log.error("Error processing webhook payload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing payload");
        }

        return ResponseEntity.ok("Event received");
    }

    private void handleIssueComment(String payload) throws Exception {
        IssueCommentPayload issueComment = objectMapper.readValue(payload, IssueCommentPayload.class);
        if (!"created".equals(issueComment.getAction()) || !issueComment.isPullRequest()) {
            return;
        }

        Optional<CommandContext> parsedCommand = commandParser.parse(
                issueComment.getComment().getBody(),
                issueComment.getInstallation().getId(),
                issueComment.getRepository().getOwner().getLogin(),
                issueComment.getRepository().getName(),
                issueComment.getIssue().getNumber(),
                issueComment.getComment().getId(),
                issueComment.getSender().getLogin()
        );

        parsedCommand.ifPresent(eventProcessor::processCommand);
    }

    private void handlePullRequest(String payload) throws Exception {
        PullRequestPayload prPayload = objectMapper.readValue(payload, PullRequestPayload.class);
        String action = prPayload.getAction();

        if ("opened".equals(action)) {
            try {
                GHRepository ghRepo = gitHubApiService.getRepository(
                        prPayload.getInstallation().getId(),
                        prPayload.getRepository().getOwner().getLogin(),
                        prPayload.getRepository().getName()
                );
                RepoConfig config = repoConfigService.loadConfig(ghRepo);

                if (config.isAutoReview()) {
                    CommandContext reviewContext = new CommandContext(
                            "review",
                            "",
                            prPayload.getInstallation().getId(),
                            prPayload.getRepository().getOwner().getLogin(),
                            prPayload.getRepository().getName(),
                            prPayload.getNumber(),
                            0L,
                            prPayload.getSender().getLogin()
                    );
                    eventProcessor.processCommand(reviewContext);
                }

                if (config.isAutoSummarize()) {
                    CommandContext summarizeContext = new CommandContext(
                            "summarize",
                            "",
                            prPayload.getInstallation().getId(),
                            prPayload.getRepository().getOwner().getLogin(),
                            prPayload.getRepository().getName(),
                            prPayload.getNumber(),
                            0L,
                            prPayload.getSender().getLogin()
                    );
                    eventProcessor.processCommand(summarizeContext);
                }
            } catch (Exception e) {
                log.error("Error handling auto-review/summarize for opened PR #{}", prPayload.getNumber(), e);
            }
        } else if ("synchronize".equals(action)) {
            // Triggered when developer pushes commits or applies fixes to the PR branch
            eventProcessor.processPrSynchronized(
                    prPayload.getInstallation().getId(),
                    prPayload.getRepository().getOwner().getLogin(),
                    prPayload.getRepository().getName(),
                    prPayload.getNumber(),
                    prPayload.getSender().getLogin()
            );
        }
    }
}
