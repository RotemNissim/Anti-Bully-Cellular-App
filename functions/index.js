import * as functions from "firebase-functions";

export const flagMessage = functions.https.onRequest((req, res) => {
    if (req.method !== "POST") {
        return res.status(405).send("Method Not Allowed");
    }

    const { messageId, userId, text, imageUrl } = req.body;

    if (!messageId || !userId || (!text && !imageUrl)) {
        return res.status(400).json({ error: "Invalid request" });
    }

    // Randomly flag the message
    const flagged = Math.random() < 0.5; // 50% chance of being flagged
    const reason = flagged ? "Potential bullying detected" : null;

    return res.json({
        messageId,
        flagged,
        reason,
    });
});
