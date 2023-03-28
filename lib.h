#pragma once

#include <vector>
#include <functional>
#include <map>
#include <cstdio>
#include <string>
#include "utils.h"

// determine number of model parts based on the dimension
static const std::map<int, int> LLAMA_N_PARTS = {
    { 4096, 1 },
    { 5120, 1 },
    { 6656, 4 },
    { 8192, 8 },
};

// default hparams (LLaMA 7B)
struct llama_hparams {
    int32_t n_vocab = 32000;
    int32_t n_ctx   = 512;   // this is provided as user input?
    int32_t n_embd  = 4096;
    int32_t n_mult  = 256;
    int32_t n_head  = 32;
    int32_t n_layer = 32;
    int32_t n_rot   = 64;
    int32_t f16     = 1;
};

struct llama_layer {
    // normalization
    struct ggml_tensor * attention_norm;

    // attention
    struct ggml_tensor * wq;
    struct ggml_tensor * wk;
    struct ggml_tensor * wv;
    struct ggml_tensor * wo;

    // normalization
    struct ggml_tensor * ffn_norm;

    // ff
    struct ggml_tensor * w1;
    struct ggml_tensor * w2;
    struct ggml_tensor * w3;
};

struct llama_model {
    llama_hparams hparams;

    struct ggml_tensor * tok_embeddings;

    struct ggml_tensor * norm;
    struct ggml_tensor * output;

    std::vector<llama_layer> layers;

    // key + value memory
    struct ggml_tensor * memory_k;
    struct ggml_tensor * memory_v;

    //
    struct ggml_context * ctx;
    std::map<std::string, struct ggml_tensor *> tensors;
};

// load the model's weights from a file
bool llama_model_load(const std::string & fname, llama_model & model, gpt_vocab & vocab, int n_ctx);

// evaluate the transformer
//
//   - model:     the model
//   - n_threads: number of threads to use
//   - n_past:    the context size so far
//   - embd_inp:  the embeddings of the tokens in the context
//   - embd_w:    the predicted logits for the next token
//
// The GPT-J model requires about 16MB of memory per input token.
//
bool llama_eval(
        const llama_model & model,
        const int n_threads,
        const int n_past,
        const std::vector<gpt_vocab::id> & embd_inp,
              std::vector<float>         & embd_w,
              size_t                     & mem_per_token);

enum BotStatus {
    ST_UNKNOWN = 0,
    ST_OK = 1,
    ST_FAILED = -1
};

class ChatBot {
public:
    ChatBot(
        const gpt_params& params,
        const char* instruct_str = " Below is an instruction that describes a task. Write a response that appropriately completes the request.\n\n",
        const char* prompt_str = "### Instruction:\n\n",
        const char* response_str = "### Response:\n\n"
    );
    ~ChatBot();
    BotStatus load_model();
    std::string get_answer(const std::string& question, BotStatus& infer_status,
        std::function<void(const std::string&, const BotStatus&)> emitCallback);
    inline BotStatus status() { return load_status_; }

private:
    BotStatus load_status_;
    gpt_params params_;
    std::mt19937 rng_;
    gpt_vocab vocab_;
    llama_model model_;
    int n_past_;
    std::vector<float> logits_;
    std::vector<gpt_vocab::id> embd_inp_;
    std::vector<gpt_vocab::id> embd_;
    size_t mem_per_token_;
    std::vector<gpt_vocab::id> last_n_tokens_;
    int input_consumed_;

    const char* instruct_str_;
    const char* prompt_str_;
    const char* response_str_;
    std::vector<gpt_vocab::id> instruct_inp_;
    std::vector<gpt_vocab::id> prompt_inp_;
    std::vector<gpt_vocab::id> response_inp_;
};
