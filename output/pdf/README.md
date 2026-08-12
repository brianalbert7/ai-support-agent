# RAG demo documents

These PDFs contain original, fictional content created for the AI Support Agent portfolio demo. They are safe to publish and are intentionally organized so document and page citations are easy to verify.

## Suggested display names

| File | Display name |
| --- | --- |
| `northstar-employee-handbook.pdf` | Northstar Employee Handbook |
| `northstar-customer-support-playbook.pdf` | Northstar Customer Support Playbook |
| `clouddesk-administrator-guide.pdf` | CloudDesk Administrator Guide |

Upload and process each document separately before asking questions.

## Questions with clear answers

| Question | Expected source |
| --- | --- |
| How many vacation days do full-time employees receive? | Employee Handbook, page 2 |
| How many vacation days can I carry over, and when do they expire? | Employee Handbook, page 2 |
| What are the core collaboration hours? | Employee Handbook, page 3 |
| How much is the home-office equipment stipend? | Employee Handbook, page 3 |
| How long is a password reset link valid? | Support Playbook, page 2 |
| What happens after five failed sign-in attempts? | Support Playbook, page 2 |
| How quickly must Priority 1 incidents be acknowledged? | Support Playbook, page 1 |
| Can a support specialist approve a $400 refund? | Support Playbook, page 3 |
| How long are CloudDesk backups retained? | Administrator Guide, page 2 |
| What should an API client do after receiving HTTP 429? | Administrator Guide, page 3 |
| How long does CloudDesk remember an idempotency key? | Administrator Guide, page 3 |
| How long are Enterprise audit events retained? | Administrator Guide, page 4 |

## Follow-up conversation test

1. Ask: `How many vacation days do employees receive?`
2. Follow up: `How many of those can carry into next year?`
3. Follow up: `When do the carried days expire?`

The later questions depend on conversation context but should still cite the current Employee Handbook retrieval.

## Multi-document question

Ask: `A support engineer loses a company laptop while responding to a critical incident. What deadlines apply?`

A strong answer should combine the one-hour lost-device reporting rule from Employee Handbook page 4 with the Priority 1 response and update timing from Support Playbook page 1.

## Insufficient-context test

Ask: `What dental insurance provider does the company use?`

None of these documents contains that information, so the application should return its insufficient-context response instead of inventing an answer.
