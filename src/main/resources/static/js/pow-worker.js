const BATCH = 512;

self.onmessage = async ({ data: { challenge, difficulty } }) => {
    const prefix = '0'.repeat(difficulty);
    const enc    = new TextEncoder();
    let nonce    = 0;

    while (true) {
        const indices = Array.from({ length: BATCH }, (_, i) => nonce + i);

        const results = await Promise.all(
            indices.map(n =>
                crypto.subtle
                    .digest('SHA-256', enc.encode(`${challenge}:${n}`))
                    .then(buf => ({
                        nonce: n,
                        hash: [...new Uint8Array(buf)]
                            .map(b => b.toString(16).padStart(2, '0'))
                            .join('')
                    }))
            )
        );

        for (const { nonce: n, hash } of results) {
            if (hash.startsWith(prefix)) {
                self.postMessage({ nonce: n.toString() });
                return;
            }
        }

        nonce += BATCH;
    }
};
