package com.brainserve.oysmail.service;

import com.brainserve.oysmail.model.MailBlock;
import com.brainserve.oysmail.model.EmailTheme;
import com.brainserve.oysmail.model.SocialLink;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailHtmlBuilderTest {
    private final EmailHtmlBuilder builder = new EmailHtmlBuilder();

    @Test
    void keepsOriginalBannerInlineWhenEnabled() {
        String html = builder.render(List.of(
                new MailBlock("text", "Dear student", "Study material attached.", null, null, null)
        ), true, "#f45127");
        assertThat(html).contains("src=\"cid:oys-hero\"");
        assertThat(html).contains("Dear student");
        assertThat(html).contains("Study material attached.");
    }

    @Test
    void removesBannerAndEscapesUserText() {
        String html = builder.render(List.of(
                new MailBlock("text", "<script>alert(1)</script>", "Hi & welcome", null, null, null)
        ), false, "#f45127");
        assertThat(html).doesNotContain("cid:oys-hero", "<script>");
        assertThat(html).contains("&lt;script&gt;alert(1)&lt;/script&gt;", "Hi &amp; welcome");
    }

    @Test
    void socialFooterRendersOnlySuppliedLinksAndEscapesHeadingAndUrls() {
        var links = List.of(
                new SocialLink("linkedin", "https://www.linkedin.com/company/brainserve?a=1&b=2"),
                new SocialLink("youtube", "https://www.youtube.com/@brainserve")
        );
        String html = builder.render(List.of(
                new MailBlock("text", "Hello", "Welcome", null, null, null)
        ), true, "#f45127", "Connect & learn", links);
        assertThat(html).contains("Connect &amp; learn", "LinkedIn", "YouTube");
        assertThat(html).contains("href=\"https://www.linkedin.com/company/brainserve?a=1&amp;b=2\"");
        assertThat(html).doesNotContain("Instagram", "href=\"https://example.com");
        String plain = builder.plainText(List.of(), "Connect & learn", links);
        assertThat(plain).contains("Connect & learn", "LinkedIn: https://", "YouTube: https://");
    }

    @Test
    void omitsSocialFooterWhenNoLinksAreConfigured() {
        String html = builder.render(List.of(
                new MailBlock("text", null, "Hello", null, null, null)
        ), false, "#f45127", "Connect with Brainserve", List.of());
        assertThat(html).doesNotContain("Connect with Brainserve", "f3d8c9");
    }

    @Test
    void usesInlineCidForAdditionalImagesAndButtonLink() {
        var blocks = List.of(
                new MailBlock("image", null, null, 0, null, null),
                new MailBlock("button", null, null, null, "Start learning", "https://example.org/?a=1&b=2"),
                new MailBlock("divider", null, null, null, null, null)
        );
        String html = builder.render(blocks, true, "#124F99");
        assertThat(html).contains("cid:oys-inline-0", "background:#124F99");
        assertThat(html).contains("href=\"https://example.org/?a=1&amp;b=2\"");
        assertThat(builder.plainText(blocks)).contains("Start learning: https://example.org");
    }
    @Test
    void rendersEachManualBrandColorInEmailHtml() {
        var palette = new EmailTheme("#111111", "#222222", "#333333", "#444444",
                "#555555", "#666666", "#777777", "#888888", "#999999");
        var blocks = List.of(
                new MailBlock("text", "New material", "Attached", null, null, null),
                new MailBlock("button", null, null, null, "Open resource", "https://example.org"),
                new MailBlock("notice", "Important", "Reminder", null, null, null),
                new MailBlock("divider", null, null, null, null, null));
        String html = builder.render(blocks, true, palette, "Follow our channels",
                List.of(new SocialLink("website", "https://example.org")));
        for (String color : List.of("#111111", "#222222", "#333333", "#444444",
                "#555555", "#666666", "#777777", "#888888", "#999999")) {
            assertThat(html).contains(color);
        }
        assertThat(html).contains("Reminder", "Follow our channels", "cid:oys-hero");
        assertThat(builder.plainText(blocks)).contains("Important", "Reminder");
    }

    @Test
    void noticeEscapesHtmlInsteadOfRenderingItAsCode() {
        String html = builder.render(List.of(
                new MailBlock("notice", "<script>x</script>", "<b>Text</b>", null, null, null)
        ), false, EmailTheme.defaults(), "", List.of());
        assertThat(html).contains("border-left:4px solid:#EF4444", "&lt;script&gt;", "&lt;b&gt;");
        assertThat(html).doesNotContain("<script>", "<b>Text</b>");
    }


    @Test
    void brandLogoIsIncludedAboveHeroAndSurvivesHeroBeingHidden() {
        String withHero = builder.render(List.of(new MailBlock("text", null, "Hello", null, null, null)),
                true, "#F97316");
        assertThat(withHero).contains("src=\"cid:brainserve-logo\"", "src=\"cid:oys-hero\"");
        assertThat(withHero.indexOf("cid:brainserve-logo")).isLessThan(withHero.indexOf("cid:oys-hero"));
        String noHero = builder.render(List.of(new MailBlock("text", null, "Hello", null, null, null)),
                false, "#F97316");
        assertThat(noHero).contains("cid:brainserve-logo").doesNotContain("cid:oys-hero");
    }

    @Test
    void socialImagesAndLabelsShareClickableHttpsAnchors() {
        var links = List.of(
                new SocialLink("linkedin", "https://www.linkedin.com/company/brainserve?a=1&b=2"),
                new SocialLink("youtube", "https://www.youtube.com/@brainserve"));
        String html = builder.render(List.of(new MailBlock("text", null, "Hi", null, null, null)),
                true, EmailTheme.defaults(), "Follow us", links);
        assertThat(html).contains("<a href=\"https://www.linkedin.com/company/brainserve?a=1&amp;b=2\"")
                .contains("src=\"cid:social-linkedin\"")
                .contains("src=\"cid:social-youtube\"")
                .contains("alt=\"LinkedIn\"", "alt=\"YouTube\"")
                .doesNotContain("cid:social-instagram");
        // Both the icon and its fallback text must appear before their corresponding closing anchor.
        int linkedInAnchor = html.indexOf("href=\"https://www.linkedin.com/company/");
        int linkedInIcon = html.indexOf("cid:social-linkedin");
        int linkedInClose = html.indexOf("</a>", linkedInAnchor);
        assertThat(linkedInAnchor).isLessThan(linkedInIcon);
        assertThat(linkedInIcon).isLessThan(linkedInClose);
    }
}
