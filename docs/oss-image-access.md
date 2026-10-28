# OSS image access

Business images are stored in OSS. Resource rows keep `image_uri`; the removed
`profile_image` BLOB columns must not be restored.

The frontend calls authenticated `POST /api/onepiece/image/get_image_url` with
`{"uri":"<stored image_uri>","full_image":false}`. The response contains
`url`, `original_url`, and `expires_at` (Unix milliseconds). Both URLs expire
after 15 minutes. `url` uses the existing 100 px OSS thumbnail processing unless
`full_image` is true. The frontend downloads image bytes directly from OSS,
refreshes expired signatures, and falls back to the signed original when image
processing fails. App bearer tokens are never attached to OSS requests.

The original `get_image` API remains available for older clients, but the updated
frontend does not use it for image downloads. PDF reports continue to read OSS
from the backend when assembling the document.

## Bucket CORS

Private objects remain private: signed GET URLs do not require public bucket
ACLs. Browser reads need an OSS CORS rule for the app origin. With the existing
deployment OSS credentials supplied as environment variables, run:

```sh
python3 tools/configure_oss_cors.py
```

This preserves existing rules and adds GET/HEAD for the production apex and
www origins. For local development, add only the origin actually used:

```sh
python3 tools/configure_oss_cors.py --origin http://local.onepiece-battles.com:1234
```

The credentials need `oss:GetBucketCors` and `oss:PutBucketCors` for this setup
step. Runtime URL signing uses the existing object-read credentials; CORS
administration is not performed by the application on startup.

Deploy the backend before the frontend so the signing endpoint exists when the
new image loader starts. Configure CORS before enabling the frontend change.

See [Alibaba Cloud's CORS documentation](https://www.alibabacloud.com/help/en/oss/user-guide/configure-cross-origin-resource-sharing)
for browser matching rules and troubleshooting.
